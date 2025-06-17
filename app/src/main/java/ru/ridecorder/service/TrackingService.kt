package ru.ridecorder.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.ridecorder.MainActivity
import ru.ridecorder.R
import ru.ridecorder.data.local.HeartRateMonitorManager
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.preferences.SettingsDataStore
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.di.ResourceProvider
import ru.ridecorder.domain.tracking.RoutePointsManager
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    @Inject
    lateinit var resourceProvider: ResourceProvider

    @Inject
    lateinit var routePointsManager: RoutePointsManager

    @Inject
    lateinit var heartRateMonitorManager: HeartRateMonitorManager

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    @Inject
    lateinit var workoutRepository: WorkoutRepository

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val minimumAccuracy = 15.0f // 15 метров - минимальная точность
    private val maxSpeedMs = 84.0f // 300 km/h - максимальная скорость
    private val maxSpeedChangeMs = 15f // 54 km/h - максимальное изменение скорости между точками
    private val startMaxSpeedMs = 10f // 30 km/h - максимальная скорость первой точки

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var nextRoutePointIsPaused: Boolean = false

    private var workoutId: Long = -1

    private var isRunning = false

    private var barometerManager: BarometerManager? = null

    // --- Интеграция пульсометра ---
    // Храним последнее значение пульса и время его обновления
    private var lastHeartRate: Int? = null
    private var lastHeartRateTimestamp: Long = 0L
    // Порог свежести пульса, например 10 секунд (10000 мс)
    private val heartRateFreshnessThreshold = 10000L


    // Переменная для хранения текущего значения авто-паузы
    private var autoPauseEnabled = false
    // Порог скорости для определения простоя (например, 0.5 м/с)
    private val idleSpeedThreshold = 0.5f

    override fun onCreate() {
        super.onCreate()

        barometerManager = BarometerManager(this)
        barometerManager?.start()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Запускаем сервис как foreground
        startForeground(NOTIFICATION_ID, createNotification())

        // Подписываемся на поток пульса через lifecycleScope.
        // Все новые показания обновляют lastHeartRate и время обновления.
        lifecycleScope.launch(Dispatchers.IO) {
            heartRateMonitorManager.heartRateFlow.collect { heartRate ->
                lastHeartRate = heartRate
                lastHeartRateTimestamp = System.currentTimeMillis()
                Log.d("TrackingService", "Получен новый пульс: $heartRate")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isRunning = true

        val prefs = getSharedPreferences("service_prefs", Context.MODE_PRIVATE)

        // Получаем workoutId
        // Если intent есть, обновляем workoutId и сохраняем в SharedPreferences
        if (intent != null) {
            workoutId = intent.getLongExtra(WORKOUT_ID, -1)
            prefs.edit().putLong(WORKOUT_ID, workoutId).apply()
        } else {
            // Если intent == null, восстанавливаем workoutId и RoutePointsRepository
            workoutId = prefs.getLong(WORKOUT_ID, -1)
            if(workoutId != -1L) {
                // при перезапуске сервиса (когда intent равен null) текущие точки маршрута
                // будут подгружены из Room, а Repository обновлён соответствующим образом
                lifecycleScope.launch {
                    val routePoints = workoutRepository
                        .getAllRoutePoints(workoutId)
                        .first()
                    // Устанавливаем точки в репозиторий маршрута
                    routePointsManager.setPoints(routePoints)
                }
            }
        }

        if (workoutId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Если это возобновление записи (с паузы),
        // то нужно не считать следующий промежуток в статистику
        val isResume = intent?.getBooleanExtra(IS_RESUME, false) ?: false
        if(isResume)
            nextRoutePointIsPaused = true;

        // Подписываемся на поток настроек авто-паузы
        lifecycleScope.launch {
            settingsDataStore.pauseOnIdleFlow.collect { pauseEnabled ->
                autoPauseEnabled = pauseEnabled
            }
        }

        // Подключаем устройство пульсометра, если оно выбрано
        lifecycleScope.launch {
            // Если требуется подключение один раз, можно использовать first()
            val selectedDevice = settingsDataStore.selectedDeviceFlow.first()
            if (selectedDevice.isNotEmpty()) {
                heartRateMonitorManager.connectToDevice(selectedDevice)
                Log.d("TrackingService", "Подключились к устройству: $selectedDevice")
            } else {
                Log.d("TrackingService", "Устройство не выбрано")
            }
        }

        // Запускаем подписку на обновления локации
        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (!checkPermissions()) {
            stopSelf()
            return
        }

        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1500)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.accuracy > minimumAccuracy) {
                        Log.d("LocationUpdate", "Точка не добавлена, низкая точность: ${location.accuracy} м")
                        routePointsManager.setIsBadAccuracy(true)
                        return
                    }
                    if(routePointsManager.routePointsFlow.value.any()) {
                        val prevLocation = routePointsManager.routePointsFlow.value.last()

                        val speedChange = abs(prevLocation.speed - location.speed)
                        if(speedChange > maxSpeedChangeMs) {
                            Log.d("LocationUpdate", "Точка отклонена: скорость изменилась более чем на ${maxSpeedChangeMs} - ${speedChange} м/c")
                            routePointsManager.setIsBadAccuracy(true)
                            return
                        }
                        val distance = prevLocation.distanceTo(location)
                        val timeDeltaSeconds = (System.currentTimeMillis() - prevLocation.timestamp) / 1000.0

                        // Максимально допустимое расстояние
                        val maxDistance = maxSpeedMs * timeDeltaSeconds
                        if (distance > maxDistance) {
                            Log.d("LocationUpdate", "Точка отклонена: расстояние $distance м превышает максимальное за это время - $maxDistance м")
                            routePointsManager.setIsBadAccuracy(true)
                            return
                        }
                    }
                    else {
                        if(location.speed > startMaxSpeedMs) {
                            Log.d("LocationUpdate", "Точка отклонена: начальная скорость больше ${startMaxSpeedMs} - ${location.speed} м/c")
                            routePointsManager.setIsBadAccuracy(true)
                            return
                        }
                    }
                    if (location.speed > maxSpeedMs) {
                        Log.d("LocationUpdate", "Нереалистичная скорость: ${location.speed} м/с")
                        routePointsManager.setIsBadAccuracy(true)
                        return
                    }

                    if(abs(location.time - System.currentTimeMillis()) > 5 * 1000) {
                        Log.d("LocationUpdate", "Устаревшие GPS данные: ${location.time} <> ${System.currentTimeMillis()} м/с")
                        routePointsManager.setIsBadAccuracy(true)
                        return
                    }

                    // Если настройка авто-паузы включена, проверяем скорость и определяем, следует ли отмечать точку как паузу
                    if (autoPauseEnabled) {
                        nextRoutePointIsPaused = location.speed < idleSpeedThreshold
                    }

                    routePointsManager.setIsBadAccuracy(false)

                    val heartRateForPoint =
                        if ((System.currentTimeMillis() - lastHeartRateTimestamp) > heartRateFreshnessThreshold) null else lastHeartRate

                    val point = RoutePointEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = location.speed,     // м/с
                        altitude = location.altitude,
                        bearing = location.bearing,
                        timestamp = System.currentTimeMillis(),
                        isPause = nextRoutePointIsPaused,
                        workoutId = workoutId,

                        provider = location.provider,

                        accuracy = location.accuracy,
                        verticalAccuracyMeters = location.verticalAccuracyMeters,
                        bearingAccuracyDegrees = location.bearingAccuracyDegrees,
                        speedAccuracyMetersPerSecond = location.speedAccuracyMetersPerSecond,

                        barometerAltitude = barometerManager?.getLastAltitude(),

                        heartRate = heartRateForPoint
                    )
                    if (nextRoutePointIsPaused)
                        nextRoutePointIsPaused = false

                    lifecycleScope.launch {
                        val pointWasAdded = routePointsManager.addPoint(point)
                        if(pointWasAdded){
                            workoutRepository.insertRoutePoints(
                                listOf(
                                    point
                                )
                            )
                        }
                    }
                }
            }
        }


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("TrackingService", "No permissions")
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createNotification(): Notification {
        val channelId = "tracking_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        // Интент для открытия или разворачивания MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(resourceProvider.getString(R.string.recording_notification_title))
            .setContentText(resourceProvider.getString(R.string.recording_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent) // Разворачивает приложение
            .setOngoing(true) // Делаем уведомление не закрываемым
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }


    fun isRunning() : Boolean {
        return isRunning
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        barometerManager?.stop()
        heartRateMonitorManager.disconnect()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 85923
        const val WORKOUT_ID = "extra_workout_id"
        const val IS_RESUME = "extra_is_resume"

        fun start(context: Context, workoutId: Long) {
            val intent = Intent(context, TrackingService::class.java).apply {
                putExtra(WORKOUT_ID, workoutId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context, workoutId: Long) {
            val intent = Intent(context, TrackingService::class.java).apply {
                putExtra(WORKOUT_ID, workoutId)
                putExtra(IS_RESUME, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }


        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            
            context.stopService(intent)
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

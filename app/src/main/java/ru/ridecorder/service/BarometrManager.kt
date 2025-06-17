package ru.ridecorder.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class BarometerManager(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // Получаем барометр (датчик давления), если он доступен
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private var lastBarometerAltitude: Float? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // Значение давления в гПа, преобразуем в высоту в метрах
                val pressure = it.values[0]
                lastBarometerAltitude = SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                    pressure
                )
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Запускаем слушатель барометра, если датчик есть
    fun start() {
        pressureSensor?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Останавливаем слушатель
    fun stop() {
        sensorManager.unregisterListener(sensorListener)
    }

    // Возвращает последнюю полученную высоту с барометра или null, если датчик отсутствует или данные еще не получены
    fun getLastAltitude(): Float? {
        return lastBarometerAltitude
    }
}

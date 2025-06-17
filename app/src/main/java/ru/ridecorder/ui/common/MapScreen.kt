package ru.ridecorder.ui.common

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import ru.ridecorder.R
import ru.ridecorder.data.local.database.RoutePointEntity

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    points: List<RoutePointEntity>,
    badAccuracy: Boolean = false,
    showCurrentLocation: Boolean = true,
    defaultZoom: Float = 18f,
    defaultPoint: Point? = null,
    darkMode: Boolean = false
) {
    // Локальный контекст из Compose
    val context = LocalContext.current
    // Получаем текущий Lifecycle (чтобы отсылать onStart/onStop)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Создаём MapView
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    LaunchedEffect(Unit) {
        mapViewState.value = MapView(context).apply {
            // Можно задать базовые настройки карты сразу после создания, если требуется.
        }
    }

    // Пока карта не создана, показываем индикатор загрузки
    if (mapViewState.value == null) {
        LoadingScreen()
        return
    }

    val mapView = mapViewState.value!!

    val isMapShow = remember { mutableStateOf(false) }

    // Храним количество уже добавленных точек
    var previousPointsCount by remember { mutableIntStateOf(1) }

    val userLocationLayer = remember {
        if(showCurrentLocation) {
            MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow).apply {
                isVisible = true
                isHeadingEnabled = true
                //  isAutoZoomEnabled = true
            }
        }
        else null
    }
    val followUserLocation = remember { mutableStateOf<Boolean?>(null) }

    fun cameraUserPosition() {
        userLocationLayer?.cameraPosition()?.let { userPos ->
            mapView.mapWindow.map.move(
                CameraPosition(userPos.target, defaultZoom, userPos.azimuth, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        }
    }

    fun setAnchor() {
        userLocationLayer?.setAnchor( // TODO: сделать плавное перемещение камеры
            PointF((mapView.width * 0.5f), (mapView.height * 0.6f)),
            PointF((mapView.width * 0.5f), (mapView.height * 0.7f))
        )
    }
    fun noAnchor() {
        userLocationLayer?.resetAnchor()
    }


    val userLocationListener = remember {
        object : UserLocationObjectListener {
            override fun onObjectAdded(userLocationView: UserLocationView) {
                userLocationView.pin.setIcon(
                    ImageProvider.fromResource(context, R.drawable.ic_user_location)
                )
                userLocationView.arrow.setIcon(
                    ImageProvider.fromResource(context, R.drawable.ic_user_location)
                )
                val pinIcon = userLocationView.pin.useCompositeIcon()

                pinIcon.setIcon(
                    "ic_user_location",
                    ImageProvider.fromResource(context, R.drawable.ic_user_location),
                    IconStyle().setAnchor(PointF(0f, 0f))
                        .setRotationType(RotationType.ROTATE)
                        .setZIndex(0f)
                        .setScale(1f)
                )
                // Когда иконка появляется впервые, двигаем камеру на пользователя
                if (followUserLocation.value == null) {
                    setAnchor()
                    followUserLocation.value = true;
                }
            }



            override fun onObjectRemoved(view: UserLocationView) {}
            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
        }
    }


    // Слушатель камеры
    val cameraListener = remember {
        object : CameraListener {
            override fun onCameraPositionChanged(
                map: com.yandex.mapkit.map.Map,
                cPos: CameraPosition,
                cUpd: CameraUpdateReason,
                finish: Boolean
            ) {
                // Если пользователь сам двигал карту (жесты):
                if (cUpd == CameraUpdateReason.GESTURES) {
                    followUserLocation.value = false
                    noAnchor()
                }
            }
        }
    }

    LaunchedEffect(points) {
        val map = mapView.mapWindow.map
        val mapObjects = map.mapObjects

        if (points.size < previousPointsCount)
        {
            mapObjects.clear()
            previousPointsCount = 1
        }

        if (points.size > previousPointsCount && points.size > 1) {
            val newPoints = points
                .drop(previousPointsCount - 1) // Берём только новые точки
                .map { Point(it.latitude, it.longitude) }

            if (newPoints.isNotEmpty()) {
                // Добавляем новые точки к существующей полилинии
                val polyline = Polyline(newPoints.map { Point(it.latitude, it.longitude) })
                val polylineMapObject = mapObjects.addPolyline(polyline)

                // Обновляем количество уже добавленных точек
                previousPointsCount = points.size
            }
        }
    }

    // Следим за жизненным циклом (через DisposableEffect + LifecycleObserver)
    DisposableEffect(lifecycle, mapView) {
        // Создаём observer для onStart/onStop
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // То же, что вы делали в Activity.onStart
                MapKitFactory.getInstance().onStart()
                mapView.onStart()
                isMapShow.value = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isMapShow.value = false
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
            }
        }
        // Добавляем observer
        lifecycle.addObserver(lifecycleObserver)

        // Когда Composable уходит из Composition, удалим observer
        onDispose {
             lifecycle.removeObserver(lifecycleObserver)
         }
    }
    

    Box(
        modifier = Modifier.fillMaxSize()
            .alpha(if (isMapShow.value) 1f else 0f),
    ) {

        AndroidView(
            factory =
            {
                if(showCurrentLocation){
                    userLocationLayer?.setObjectListener(userLocationListener)
                    mapView.mapWindow.map.addCameraListener(cameraListener)
                }
                mapView.mapWindow.map.isNightModeEnabled = darkMode

                mapView.mapWindow.map.set2DMode(true)
                mapView.mapWindow.map.poiLimit = 0
                mapView.mapWindow.map.move(
                    CameraPosition(
                        defaultPoint ?: Point(55.751225, 37.62954),
                        defaultZoom,
                        0.0f,
                        0.0f
                    )
                )
                mapView
            },
            modifier = modifier,
        )



        if(showCurrentLocation) {
            FloatingActionButton(
                onClick = {
                    followUserLocation.value = true
                    cameraUserPosition()
                    setAnchor()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(id = R.string.my_geoposition)
                )
            }
        }
        AnimatedVisibility(
            visible = badAccuracy,
            enter = slideInVertically(initialOffsetY = { -it }),  // выезжает сверху
            exit = slideOutVertically(targetOffsetY = { -it })     // скрывается обратно вверх
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow)
                    .padding(vertical = 5.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.gps_bad_accuracy),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
            }
        }
    }
}

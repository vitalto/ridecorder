package ru.ridecorder.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mapkit.geometry.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.GpxService
import ru.ridecorder.data.local.database.WorkoutWithPoints
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.remote.network.models.workout.toEntity
import ru.ridecorder.domain.analysis.RouteAnalytics
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.ui.helpers.MapHelper
import javax.inject.Inject

@HiltViewModel
class DetailedWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val gpxService: GpxService,
    private val userDataStore: UserDataStore
) : ViewModel() {

    private val _workoutWithPoints = MutableStateFlow<WorkoutWithPoints?>(null)
    val workoutWithPoints: StateFlow<WorkoutWithPoints?> = _workoutWithPoints.asStateFlow()

    private val _cameraData = MutableStateFlow<MapHelper.CameraData?>(null)
    val cameraData: StateFlow<MapHelper.CameraData?> = _cameraData.asStateFlow()

    private val _routeAnalytics = MutableStateFlow<RouteAnalytics?>(null)
    val routeAnalytics: StateFlow<RouteAnalytics?> = _routeAnalytics.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadWorkout(
        workoutId: Long,
        screenWidth: Float,
        screenHeight: Float,
        isUserWorkout: Boolean
    ) {
        _errorMessage.value = null
        if(isUserWorkout){
            loadLocalWorkout(workoutId, screenWidth, screenHeight)
            return
        }
        loadServerWorkout(workoutId.toInt(), screenWidth, screenHeight)
    }
    private fun loadLocalWorkout(
        workoutId: Long,
        screenWidth: Float,
        screenHeight: Float,
    ) {
        viewModelScope.launch {

            val workoutWithPoints = workoutRepository.getWorkoutWithPoints(workoutId)
            _workoutWithPoints.value = workoutWithPoints

            workoutWithPoints?.let { wkp ->
                val points = wkp.points
                val gender = userDataStore.userFlow.firstOrNull()?.gender
                _routeAnalytics.value = RouteStatsCalculator.analyze(wkp.workout, points, gender)

                _cameraData.value = MapHelper.calculateCameraData(
                    points.map { Point(it.latitude, it.longitude) },
                    (screenWidth * 0.8).toInt(),
                    (screenHeight * 0.6).toInt(),
                )
            }
        }
    }

    private fun loadServerWorkout(
        serverWorkoutId: Int,
        screenWidth: Float,
        screenHeight: Float,
    ) {
        viewModelScope.launch {
            try{
                val workoutWithPointsResponse = workoutRepository.getServerWorkoutWithPoints(serverWorkoutId)
                if(!workoutWithPointsResponse.success) throw Exception(workoutWithPointsResponse.errorMessage)

                val workoutWithPoints = workoutWithPointsResponse.data!!.toEntity()
                _workoutWithPoints.value = workoutWithPoints

                workoutWithPoints.let { wkp ->
                    val points = wkp.points
                    val gender = userDataStore.userFlow.firstOrNull()?.gender
                    _routeAnalytics.value = RouteStatsCalculator.analyze(wkp.workout, points, gender)

                    _cameraData.value = MapHelper.calculateCameraData(
                        points.map { Point(it.latitude, it.longitude) },
                        (screenWidth * 0.8).toInt(),
                        (screenHeight * 0.6).toInt(),
                    )
                }
            }catch (e: Exception){
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun exportWorkout(workoutId: Long) {
        val workoutWithPoints = _workoutWithPoints.value ?: return

        val workoutGpx = gpxService.exportWorkoutToGpx(workoutWithPoints.workout, workoutWithPoints.points)
        gpxService.saveGpxToDownloads("workout$workoutId", workoutGpx)
    }
}

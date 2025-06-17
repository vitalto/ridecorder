package ru.ridecorder.ui.recording

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.R
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import ru.ridecorder.domain.model.RideType
import ru.ridecorder.domain.model.Visibility
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.di.ResourceProvider
import ru.ridecorder.domain.tracking.RoutePointsManager
import ru.ridecorder.ui.helpers.ConvertHelper
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class RecordingSaveViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val routePointsManager: RoutePointsManager,
    private val userDataStore: UserDataStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    // Состояния текстовых полей
    var rideName = mutableStateOf("")
    var rideDescription = mutableStateOf("")

    // Списки для выпадающих меню
    val rideTypeList = listOf(RideType.TRAINING, RideType.RACE, RideType.REGULAR, RideType.OTHER)
    val whoCanViewList = listOf(Visibility.ALL, Visibility.FOLLOWERS, Visibility.NOBODY)

    // Состояния для "Тип заезда"
    var selectedRideType = mutableStateOf(rideTypeList[0])

    // Состояния для "Кто может просматривать"
    var selectedWhoCanView = mutableStateOf(whoCanViewList[0])

    /**
     * Удаляем маршрут по ID. Передаём [context],
     * так как внутри нужно остановить сервис.
     */
    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            workoutRepository.deleteWorkoutById(workoutId)
            routePointsManager.clear()
        }
    }

    /**
     * Сохраняем (обновляем) маршрут по ID.
     */
    suspend fun saveWorkout(workoutId: Long) {
        val route = workoutRepository.getWorkoutWithPoints(workoutId)
        route?.let {
            val stats = RouteStatsCalculator.calculateTrackingStats(route.points)
            val sortedPoints = route.points.sortedBy { point -> point.timestamp }
            it.workout.startTimestamp = Instant.ofEpochMilli(sortedPoints.first().timestamp)
            it.workout.endTimestamp = Instant.ofEpochMilli(sortedPoints.last().timestamp)
            it.workout.averageSpeed = stats.averageSpeed
            it.workout.duration = stats.activeDuration
            it.workout.distance = stats.distance
            it.workout.weight = userDataStore.userFlow.firstOrNull()?.weight
                it.workout.isFinished = true
            it.workout.name = rideName.value
            if(it.workout.name.isNullOrBlank())
                it.workout.name = resourceProvider.getString(R.string.ride_name_default_format,
                    ConvertHelper.formatTimestamp(resourceProvider, it.workout.startTimestamp, true)
                )

            it.workout.description = rideDescription.value
            it.workout.type = selectedRideType.value.value
            it.workout.whoCanView = selectedWhoCanView.value.value

            // Сохраняем изменения в БД
            workoutRepository.updateWorkoutLocally(it.workout)
            workoutRepository.sync()
        }
        // Очищаем репозиторий
        routePointsManager.clear()
    }
}

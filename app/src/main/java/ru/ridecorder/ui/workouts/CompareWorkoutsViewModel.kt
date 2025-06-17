package ru.ridecorder.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.database.WorkoutWithPoints
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.remote.network.models.workout.toEntity
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.domain.analysis.RouteAnalytics
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import javax.inject.Inject

@HiltViewModel
class CompareWorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userDataStore: UserDataStore
) : ViewModel() {

    private val _firstWorkout = MutableStateFlow<WorkoutWithPoints?>(null)
    val firstWorkout: StateFlow<WorkoutWithPoints?> = _firstWorkout.asStateFlow()

    private val _secondWorkout = MutableStateFlow<WorkoutWithPoints?>(null)
    val secondWorkout: StateFlow<WorkoutWithPoints?> = _secondWorkout.asStateFlow()

    private val _firstWorkoutAnalytics = MutableStateFlow<RouteAnalytics?>(null)
    val firstWorkoutAnalytics: StateFlow<RouteAnalytics?> = _firstWorkoutAnalytics.asStateFlow()

    private val _secondWorkoutAnalytics = MutableStateFlow<RouteAnalytics?>(null)
    val secondWorkoutAnalytics: StateFlow<RouteAnalytics?> = _secondWorkoutAnalytics.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Метод для загрузки данных сразу по двум тренировкам.
     * [firstIsUserWorkout] и [secondIsUserWorkout] нужны, чтобы решить,
     * откуда грузить (из локальной БД или с сервера).
     */
    fun loadWorkouts(
        firstWorkoutId: Long,
        secondWorkoutId: Long,
        firstIsUserWorkout: Boolean,
        secondIsUserWorkout: Boolean
    ) {
        _errorMessage.value = null

        viewModelScope.launch {
            val gender = userDataStore.userFlow.firstOrNull()?.gender
            // Загружаем первую тренировку
            try {
                if (firstIsUserWorkout) {
                    val localW = workoutRepository.getWorkoutWithPoints(firstWorkoutId)
                    _firstWorkout.value = localW
                    localW?.points?.let { points ->
                        _firstWorkoutAnalytics.value = RouteStatsCalculator.analyze(localW.workout, points, gender)
                    }
                } else {
                    val serverResponse = workoutRepository.getServerWorkoutWithPoints(firstWorkoutId.toInt())
                    if (!serverResponse.success) {
                        throw Exception(serverResponse.errorMessage)
                    }
                    val w = serverResponse.data!!.toEntity()
                    _firstWorkout.value = w
                    w.points.let { points ->
                        _firstWorkoutAnalytics.value = RouteStatsCalculator.analyze(w.workout, points, serverResponse.data.gender)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage
            }

            // Загружаем вторую тренировку
            try {
                if (secondIsUserWorkout) {
                    val localW = workoutRepository.getWorkoutWithPoints(secondWorkoutId)
                    _secondWorkout.value = localW
                    localW?.points?.let { points ->
                        _secondWorkoutAnalytics.value = RouteStatsCalculator.analyze(localW.workout, points, gender)
                    }
                } else {
                    val serverResponse = workoutRepository.getServerWorkoutWithPoints(secondWorkoutId.toInt())
                    if (!serverResponse.success) {
                        throw Exception(serverResponse.errorMessage)
                    }
                    val w = serverResponse.data!!.toEntity()
                    _secondWorkout.value = w
                    w.points.let { points ->
                        _secondWorkoutAnalytics.value = RouteStatsCalculator.analyze(w.workout, points, serverResponse.data.gender)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage
            }
        }
    }
}

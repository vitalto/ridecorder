package ru.ridecorder.ui.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.HeartRateMonitorManager
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.domain.tracking.RoutePointsManager
import ru.ridecorder.service.TrackingServiceManager
import javax.inject.Inject

sealed class TrainingState {
    data object Idle : TrainingState()
    data object Recording : TrainingState()
    data object Pause : TrainingState()
}

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val routePointsManager: RoutePointsManager,
    private val heartRateMonitorManager: HeartRateMonitorManager,
) : ViewModel() {

    val routePoints = routePointsManager.routePointsFlow
    val stats = routePointsManager.stats
    val isBadAccuracy = routePointsManager.badAccuracy

    val currentHeartRate = heartRateMonitorManager.heartRateFlow

    private val _trainingState = MutableStateFlow<TrainingState>(TrainingState.Idle)
    val trainingState: StateFlow<TrainingState> get() = _trainingState

    // Флаг для отображения диалога о незаконченных маршрутах
    private val _showResumeDialog = MutableStateFlow(false)
    val showResumeDialog: StateFlow<Boolean> get() = _showResumeDialog

    var currentWorkoutId: Long = 0L

    fun checkUnfinishedWorkout() {
        if(currentWorkoutId > 0) return

        viewModelScope.launch {
            val unfinishedWorkout = workoutRepository.getUnfinishedWorkout() ?: return@launch

            currentWorkoutId = unfinishedWorkout.id

            val isServiceRunning = TrackingServiceManager.isRunning()
            if(!isServiceRunning) {
                routePointsManager.clear() // Очищаем точки в кеше
                _showResumeDialog.value = true
            }
            else { // сервис отслеживает маршрут прямо сейчас
                _trainingState.value = TrainingState.Recording
            }
        }

    }

    // Обработка результата диалога: true – продолжить, false – удалить
    fun onResumeDialogResult(continueWorkout: Boolean, context: Context) {
        viewModelScope.launch {
            if (continueWorkout) {
                // Если пользователь решил продолжить, возобновляем запись с найденным workoutId
                val routePoints = workoutRepository.getAllRoutePoints(currentWorkoutId)
                    .first()

                routePointsManager.setPoints(routePoints)

                TrackingServiceManager.resumeService(context, currentWorkoutId)
                _trainingState.value = TrainingState.Recording
            } else {
                // Если пользователь выбрал удаление, удаляем маршрут из БД
                workoutRepository.deleteWorkoutById(currentWorkoutId)
                currentWorkoutId = 0L

            }
            _showResumeDialog.value = false
        }
    }


    fun start(context: Context) {
        if(_trainingState.value != TrainingState.Idle) return;
        viewModelScope.launch {
            // Очистка предыдущего маршрута
            routePointsManager.clear()

            // Вставляем новый маршрут и получаем workoutId
            currentWorkoutId = workoutRepository.insertWorkoutLocally(WorkoutEntity(isFinished = false))

            // Запускаем TrackingService с полученным workoutId
            TrackingServiceManager.startService(context, currentWorkoutId)

            // Обновляем состояние тренировки
            _trainingState.value = TrainingState.Recording
        }
    }

    fun pauseToggle(context: Context) {
        if(_trainingState.value == TrainingState.Idle) return;

        viewModelScope.launch {
            if (_trainingState.value == TrainingState.Pause) {
                resume(context)
            } else {
                pause(context)
            }
        }
    }
    fun pause(context: Context) {
        if(_trainingState.value != TrainingState.Recording) return;

        TrackingServiceManager.stopService(context);
        _trainingState.value = TrainingState.Pause
    }
    fun resume(context: Context) {
        if(_trainingState.value != TrainingState.Pause) return;

        TrackingServiceManager.resumeService(context, currentWorkoutId);
        _trainingState.value = TrainingState.Recording
    }
    fun deleteWorkoutIfEmpty(context: Context): Boolean {
        if(routePoints.value.count() <= 2) {
            viewModelScope.launch {
                reset(context, true)
            }

            return true
        }
        return false
    }
    private suspend fun reset(context: Context, deleteWorkout: Boolean){
        TrackingServiceManager.stopService(context)

        if(deleteWorkout && currentWorkoutId > 0)
            workoutRepository.deleteWorkoutById(currentWorkoutId)

        currentWorkoutId = 0
        routePointsManager.clear()

        _trainingState.value = TrainingState.Idle

    }

    fun getDuration() : Long
    {
        val startTrainingTime = (routePoints.value.firstOrNull()?.timestamp ?: System.currentTimeMillis())
        if(trainingState.value == TrainingState.Recording)
            return System.currentTimeMillis() -
                    startTrainingTime -
                    stats.value.pauseDuration
        else
            return stats.value.activeDuration
    }
}
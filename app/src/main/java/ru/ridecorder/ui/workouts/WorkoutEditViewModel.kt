package ru.ridecorder.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Inject


@HiltViewModel
class WorkoutEditViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _workout = MutableStateFlow<WorkoutEntity?>(null)
    val workout: StateFlow<WorkoutEntity?> = _workout.asStateFlow()


    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId)
            _workout.value = workout
        }
    }

    fun save(workoutEntity: WorkoutEntity) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutLocally(workoutEntity)
            workoutRepository.sync()
        }
    }

    fun delete(workoutEntity: WorkoutEntity) {
        viewModelScope.launch {
            workoutRepository.markWorkoutAsDeleted(workoutEntity.id)
            workoutRepository.sync()
        }
    }
}

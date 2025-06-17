package ru.ridecorder.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.local.database.toDto
import ru.ridecorder.data.local.preferences.User
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Inject

@HiltViewModel
class ChooseWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userDataStore: UserDataStore
) : ViewModel() {

    private val _workoutList = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    val workoutList: StateFlow<List<WorkoutEntity>> = _workoutList.asStateFlow()

    private var currentUser: User? = null

    init {
        viewModelScope.launch {
            try {
                val response = workoutRepository.getAllWorkouts().first() // Получаем список тренировок
                currentUser = userDataStore.userFlow.firstOrNull() // Получаем данные о пользователе
                _workoutList.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun workoutToDto(workout: WorkoutEntity): WorkoutDto{
        return workout.toDto(
            userId = currentUser?.userId ?: 0,
            userName = currentUser?.userName ?: "",
            userFullName = currentUser?.userFullName ?: "",
            userAvatarUrl = currentUser?.userAvatarUrl
        )
    }

}

package ru.ridecorder.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import ru.ridecorder.data.repository.FeedRepository
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    // Состояние экрана «Лента»
    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        // При инициализации подгружаем ленту
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try{
                val response = feedRepository.getFeed()
                if (response.success) {
                    // Допустим, в ApiResponse есть поле data с результатом
                    val data = response.data ?: emptyList()
                    _uiState.value = FeedUiState(
                        isLoading = false,
                        workouts = data,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = FeedUiState(
                        isLoading = false,
                        workouts = emptyList(),
                        errorMessage = response.errorMessage
                    )
                }
            }catch (e: Exception){
                _uiState.value = FeedUiState(
                    isLoading = false,
                    workouts = emptyList(),
                    errorMessage = e.message
                )
            }
        }
    }

    /**
     * Метод для постановки или снятия лайка.
     */
    fun toggleLike(workout: WorkoutDto) {
        viewModelScope.launch {
            // Сначала локально меняем состояние (UI сразу обновится)
            val currentList = _uiState.value.workouts
            val updatedList = currentList.map {
                if (it.id == workout.id) {
                    if (it.isLiked) {
                        it.copy(
                            isLiked = false,
                            likesCount = (it.likesCount - 1).coerceAtLeast(0)
                        )
                    } else {
                        it.copy(
                            isLiked = true,
                            likesCount = it.likesCount + 1
                        )
                    }
                } else {
                    it
                }
            }
            _uiState.value = _uiState.value.copy(workouts = updatedList)

            try{
                // Запрос на сервер
                if (workout.isLiked) {
                    // Если до клика была стоит галочка "isLiked = true",
                    // значит теперь пользователь хочет убрать лайк
                    workoutRepository.unlikeWorkout(workout.id)
                } else {
                    // Иначе пользователь хочет поставить лайк
                    workoutRepository.likeWorkout(workout.id)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}

data class FeedUiState(
    val isLoading: Boolean = false,
    val workouts: List<WorkoutDto> = emptyList(),
    val errorMessage: String? = null
)

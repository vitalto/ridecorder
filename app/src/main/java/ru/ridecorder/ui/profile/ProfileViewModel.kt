package ru.ridecorder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.R
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import ru.ridecorder.data.repository.ProfileRepository
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.di.ResourceProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

// Ui-статус (State), который мы будем передавать в экран.
data class MyProfileUiState(
    val userName: String = "",
    val userLogin: String = "",
    val registrationDate: String = "",
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val totalTrainings: Int = 0,
    val totalDistance: Int = 0,         // км
    val totalTime: String = "",         // например, "12 ч 45 мин"
    val averageSpeed: Float = 0f,        // км/ч
    val avatarUrl: String? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val workouts: List<WorkoutDto> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val workoutRepository: WorkoutRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    // Используем StateFlow для отслеживания изменений в UI
    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()


    fun loadUserProfile(userId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = MyProfileUiState(isLoading = true)
            try {
                val response = profileRepository.getProfile(userId)
                if (response.success) {
                    response.data?.let { userDto ->
                        // Формируем состояние экрана на основе полученных данных
                        _uiState.value = MyProfileUiState(
                            userName = userDto.fullName.orEmpty(),
                            userLogin = userDto.username.orEmpty(),
                            registrationDate = convertInstantToRegistrationDate(userDto.createdAt),
                            isFollowing = userDto.isFollowing,
                            followerCount = userDto.followerCount,
                            followingCount = userDto.followingCount,
                            totalTrainings = userDto.workoutsCount,
                            totalDistance = userDto.workoutsDistance / 1000,
                            totalTime = convertDurationToReadableTime(userDto.workoutsDuration),
                            averageSpeed = userDto.workoutsAvgSpeed,
                            avatarUrl = userDto.avatarUrl,
                            isLoading = false,
                            workouts = _uiState.value.workouts
                        )
                    }
                } else {
                    _uiState.value = MyProfileUiState(isError = true, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = MyProfileUiState(isError = true, isLoading = false)
            }
        }
    }

    fun loadUserWorkouts(userId: Int? = null) {
        if(userId == null) return
        viewModelScope.launch {
            try {
                val response = workoutRepository.getUserWorkouts(userId)
                if (!response.success) throw Exception(response.errorMessage)

                _uiState.value = _uiState.value.copy(workouts = response.data!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    /**
     * Конвертация продолжительности (в минутах или секундах)
     * в удобочитаемый формат вида "12 ч 45 мин".
     */
    private fun convertDurationToReadableTime(duration: Int): String {
        var totalMinutes = duration / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return resourceProvider.getString(R.string.format_duration, hours, minutes)
    }

    fun convertInstantToRegistrationDate(instant: Instant): String {
        // Определяем зону (по умолчанию используется системная)
        val zoneId = ZoneId.systemDefault()
        // Преобразуем Instant в локальную дату (без времени)
        val localDate = instant.atZone(zoneId).toLocalDate()
        // Задаём формат вывода
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
        // Формируем итоговую строку
        return localDate.format(formatter)
    }

    fun subscribeToUser(userId: Int) {
        viewModelScope.launch {
            try{
                val response = profileRepository.subscribe(userId)
                if(response.success)
                    _uiState.value = _uiState.value.copy(isFollowing = true)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun unsubscribeFromUser(userId: Int) {
        viewModelScope.launch {
            try{
                val response = profileRepository.unsubscribe(userId)
                if(response.success)
                    _uiState.value = _uiState.value.copy(isFollowing = false)
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }

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

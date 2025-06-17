package ru.ridecorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.preferences.TokenDataStore
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _token.value = tokenDataStore.getToken()
            _isLoading.value = false  // Завершаем загрузку
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            workoutRepository.deleteLocalWorkouts()
            tokenDataStore.clear()
            _token.value = null
        }
    }
}

package ru.ridecorder.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.R
import ru.ridecorder.data.local.preferences.TokenDataStore
import ru.ridecorder.data.remote.network.models.auth.AuthResponse
import ru.ridecorder.data.repository.AuthRepository
import ru.ridecorder.di.ResourceProvider
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val authResponse: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenDataStore: TokenDataStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> get() = _authState


    fun login(email: String, password: String) {
        if(_authState.value == AuthState.Loading) return;

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.login(email, password)

                if(!response.success || response.data == null)
                    throw Exception(response.errorMessage)

                tokenDataStore.saveToken(response.data.token)
                _authState.value = AuthState.Success(response.data)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: getAuthErrorMessage())
            }
        }
    }

    fun register(email: String, password: String) {
        if(_authState.value == AuthState.Loading) return;

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.register(email, password, null, null)

                if(!response.success || response.data == null)
                    throw Exception(response.errorMessage)

                tokenDataStore.saveToken(response.data.token)
                _authState.value = AuthState.Success(response.data)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: getAuthErrorMessage())
            }
        }
    }

    fun getAuthErrorMessage(): String {
        return resourceProvider.getString(R.string.unknown_error)
    }
}
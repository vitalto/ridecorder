package ru.ridecorder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.data.remote.network.models.profile.UserDto
import ru.ridecorder.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _users = MutableStateFlow<List<UserDto>?>(emptyList())
    val users: StateFlow<List<UserDto>?> = _users

    fun load(userId: Int? = null, following: Boolean = false){
        _users.value = emptyList()
        if(following)
            loadFollowing(userId)
        else loadFollowers(userId)
    }
    private fun loadFollowers(userId: Int? = null) {
        viewModelScope.launch {
            try {
                val response = profileRepository.getFollowers(userId)
                if (response.success) {
                    _users.value = response.data!!
                } else {
                    _users.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _users.value = null
            }
        }
    }

    private fun loadFollowing(userId: Int? = null) {
        viewModelScope.launch {
            try {
                val response = profileRepository.getFollowing(userId)
                if (response.success) {
                    _users.value = response.data!!
                } else {
                    _users.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _users.value = null
            }
        }
    }
}
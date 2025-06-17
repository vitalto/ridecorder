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
class SearchFriendsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<UserDto>?>(null)
    val searchResults: StateFlow<List<UserDto>?> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Делаем запрос в репозиторий
                val results = profileRepository.searchFriends(query)
                _searchResults.value = if(results.success) results.data else emptyList()
            } catch (e: Exception) {
                _searchResults.value = emptyList() // В случае ошибки возвращаем пустой список
            } finally {
                _isSearching.value = false
            }
        }
    }
}

package ru.ridecorder.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileDataStore: UserDataStore,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            try{
                val localProfile = profileDataStore.userFlow.firstOrNull()
                if(localProfile == null){
                    profileRepository.getProfile()
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }
}

package ru.ridecorder.data.repository

import okhttp3.MultipartBody
import okhttp3.RequestBody
import ru.ridecorder.data.local.preferences.User
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.remote.network.ProfileApi
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.profile.UserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val profileApi: ProfileApi, private val userDataStore: UserDataStore) {
    suspend fun getProfile(userId: Int? = null): ApiResponse<UserDto> {
        val profileResponse = profileApi.getProfile(userId)
        if(userId == null && profileResponse.success){
            val profile = profileResponse.data!!
            userDataStore.saveUser(User(profile.id, profile.username, profile.fullName, profile.avatarUrl, profile.weight, profile.gender))
        }
        return profileResponse
    }
    suspend fun searchFriends(query: String): ApiResponse<List<UserDto>> = profileApi.searchFriends(query)
    suspend fun subscribe(followingId: Int): ApiResponse<Boolean> = profileApi.subscribe(followingId)
    suspend fun unsubscribe(followingId: Int): ApiResponse<Boolean> = profileApi.unsubscribe(followingId)
    suspend fun getFollowers(userId: Int? = null): ApiResponse<List<UserDto>> = profileApi.getFollowers(userId)
    suspend fun getFollowing(userId: Int? = null): ApiResponse<List<UserDto>> = profileApi.getFollowing(userId)
    suspend fun updateProfile(fullName: RequestBody?, username: RequestBody?, avatar: MultipartBody.Part?, weight: RequestBody?, gender: RequestBody?, isPrivate: RequestBody?): ApiResponse<Boolean> = profileApi.updateProfile(fullName, username, avatar, weight, gender, isPrivate)
    suspend fun deleteAvatar(): ApiResponse<Boolean> = profileApi.deleteAvatar()
}

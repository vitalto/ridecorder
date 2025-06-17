package ru.ridecorder.data.remote.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.profile.UserDto

interface ProfileApi {
    @GET("api/Profile/")
    suspend fun getProfile(@Query("userId") userId: Int?): ApiResponse<UserDto>

    @GET("api/Profile/search")
    suspend fun searchFriends(@Query("query") query: String): ApiResponse<List<UserDto>>

    @POST("api/Profile/subscribe/{followingId}")
    suspend fun subscribe(@Path("followingId") followingId: Int): ApiResponse<Boolean>

    @DELETE("api/Profile/unsubscribe/{followingId}")
    suspend fun unsubscribe(@Path("followingId") followingId: Int): ApiResponse<Boolean>

    @GET("api/Profile/followers")
    suspend fun getFollowers(@Query("userId") userId: Int? = null): ApiResponse<List<UserDto>>

    @GET("api/Profile/following")
    suspend fun getFollowing(@Query("userId") userId: Int? = null): ApiResponse<List<UserDto>>

    @Multipart
    @POST("api/Profile/edit")
    suspend fun updateProfile(
        @Part("fullName") fullName: RequestBody?,
        @Part("username") username: RequestBody?,
        @Part avatar: MultipartBody.Part?,
        @Part("weight") weight: RequestBody?,
        @Part("gender") gender: RequestBody?,
        @Part("isPrivate") isPrivate: RequestBody?,
    ): ApiResponse<Boolean>

    @DELETE("api/Profile/delete-avatar")
    suspend fun deleteAvatar(): ApiResponse<Boolean>

}
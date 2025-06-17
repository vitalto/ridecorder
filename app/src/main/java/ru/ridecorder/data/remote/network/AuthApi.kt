package ru.ridecorder.data.remote.network

import retrofit2.http.Body
import retrofit2.http.POST
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.auth.LoginRequest
import ru.ridecorder.data.remote.network.models.auth.AuthResponse
import ru.ridecorder.data.remote.network.models.auth.RegisterRequest

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>
}
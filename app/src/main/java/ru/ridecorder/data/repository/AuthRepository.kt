package ru.ridecorder.data.repository

import ru.ridecorder.data.remote.network.AuthApi
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.auth.LoginRequest
import ru.ridecorder.data.remote.network.models.auth.AuthResponse
import ru.ridecorder.data.remote.network.models.auth.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
){

    suspend fun login(email: String, password: String): ApiResponse<AuthResponse> {
        return authApi.login(LoginRequest(email, password))
    }

    suspend fun register(email: String, password: String, fullName: String?, username: String?): ApiResponse<AuthResponse> {
        return authApi.register(RegisterRequest(email, password, fullName, username))
    }
}
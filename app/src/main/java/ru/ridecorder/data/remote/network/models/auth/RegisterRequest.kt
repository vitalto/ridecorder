package ru.ridecorder.data.remote.network.models.auth

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String?,
    val username: String?
)
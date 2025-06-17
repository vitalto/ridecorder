package ru.ridecorder.data.remote.network.models.auth

data class LoginRequest(
    val email: String,
    val password: String
)
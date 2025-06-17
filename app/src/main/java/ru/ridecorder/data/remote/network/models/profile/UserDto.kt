package ru.ridecorder.data.remote.network.models.profile

import com.google.gson.annotations.SerializedName
import ru.ridecorder.config.AppConfig
import java.time.Instant


data class UserDto(
    val id: Int,
    val email: String? = "",
    val fullName: String? = "",
    val username: String? = "",
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val workoutsCount: Int = 0,
    val workoutsDuration: Int = 0,
    val workoutsDistance: Int = 0,
    val workoutsAvgSpeed: Float = 0f,
    val weight: Float? = null,
    val gender: String? = null,
    val isPrivate: Boolean = false,
    @field:SerializedName("avatarUrl") private val _avatarUrl: String? = null,
    val createdAt: Instant = Instant.now()
)
{
    val avatarUrl: String
        get() = _avatarUrl?.takeIf { it.isNotBlank() }?.let { "${AppConfig.serverUrl}$it" }
            ?: "${AppConfig.serverUrl}/avatars/placeholder.jpg"

    val displayUserName: String
        get() = when {
            !fullName.isNullOrBlank() && !username.isNullOrBlank() -> "$fullName (@$username)"
            !username.isNullOrBlank() -> "@$username"
            !fullName.isNullOrBlank() -> fullName
            else -> "#$id"
        }
}
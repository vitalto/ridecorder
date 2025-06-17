package ru.ridecorder.data.remote.network.models.workout

import com.google.gson.annotations.SerializedName
import ru.ridecorder.config.AppConfig
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.local.database.WorkoutWithPoints
import java.time.Instant

data class WorkoutDto(
    val id: Int,
    val userId: Int,
    val userName: String? = "",
    val userFullName: String? = "",
    @field:SerializedName("userAvatarUrl") private val _userAvatarUrl: String? = null,
    val startTimestamp: Instant,
    val endTimestamp: Instant,
    val duration: Long,
    val distance: Float,
    val averageSpeed: Float,
    val name: String? = "",
    val description: String? = "",
    val weight: Float? = null,
    val type: String? = "",
    val whoCanView: String? = "",
    val createdAt: Instant,
    val updatedAt: Instant,
    val likesCount: Int = 0,
    val gender: String? = null,
    val isLiked: Boolean = false,
    val routePoints: List<RoutePointDto>? = null
){
    val userAvatarUrl: String
        get() = _userAvatarUrl?.takeIf { it.isNotBlank() }?.let {
            if(it.startsWith("http")) it else "${AppConfig.serverUrl}$it"
        }
            ?: "${AppConfig.serverUrl}/avatars/placeholder.jpg"

    val displayUserName: String
        get() = when {
            !userFullName.isNullOrBlank() && !userName.isNullOrBlank() -> "$userFullName (@$userName)"
            !userName.isNullOrBlank() -> "@$userName"
            !userFullName.isNullOrBlank() -> userFullName
            else -> "#$id"
        }
}

fun WorkoutDto.toEntity(): WorkoutWithPoints {
    return WorkoutWithPoints(workout = this.toWorkoutEntity(), points = this.toPointsEntity())
}

fun WorkoutDto.toWorkoutEntity(): WorkoutEntity {
    return WorkoutEntity(
        serverId = this.id,
        startTimestamp = this.startTimestamp,
        endTimestamp = this.endTimestamp,
        duration = this.duration,
        distance = this.distance,
        averageSpeed = this.averageSpeed,
        name = this.name,
        description = this.description,
        weight = this.weight,
        type = this.type,
        whoCanView = this.whoCanView,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        likesCount = this.likesCount,
        isDeleted = false,
        isFinished = true,

    )
}
fun WorkoutDto.toPointsEntity(): List<RoutePointEntity> {
    return this.routePoints?.map { it.fromRoutePointDto(-1) } ?: emptyList()
}
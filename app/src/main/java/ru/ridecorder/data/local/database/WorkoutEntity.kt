package ru.ridecorder.data.local.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey
import ru.ridecorder.data.remote.network.models.workout.RoutePointDto
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import java.time.Instant

@Entity(tableName = "workouts")
data class WorkoutEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val serverId: Int? = null,         // id на сервере
        var isFinished: Boolean = false,

        val likesCount: Int = 0,

        var startTimestamp: Instant? = null,
        var endTimestamp: Instant? = null,
        var duration: Long? = null,
        var distance: Float? = null,
        var averageSpeed: Float? = null,

        var name: String? = null,
        var description: String? = null,
        var weight: Float? = null,

        var type: String? = null,
        var whoCanView: String? = null,
        var updatedAt: Instant = Instant.now(),
        var createdAt: Instant = Instant.now(),
        var isDeleted: Boolean = false
)

fun WorkoutEntity.toDto(
        userId: Int = 0,
        userName: String? = "",
        userFullName: String? = "",
        userAvatarUrl: String? = null,
        isLiked: Boolean = false,
        routePoints: List<RoutePointDto>? = null
): WorkoutDto {
        return WorkoutDto(
                id = this.serverId ?: 0,
                userId = userId,
                userName = userName,
                userFullName = userFullName,
                _userAvatarUrl = userAvatarUrl,
                startTimestamp = this.startTimestamp ?: Instant.now(),
                endTimestamp = this.endTimestamp ?: Instant.now(),
                duration = this.duration ?: 0L,
                distance = this.distance ?: 0f,
                averageSpeed = this.averageSpeed ?: 0f,
                name = this.name ?: "",
                description = this.description ?: "",
                weight = this.weight,
                type = this.type ?: "",
                whoCanView = this.whoCanView ?: "",
                createdAt = this.createdAt,
                updatedAt = this.updatedAt,
                likesCount = this.likesCount,
                isLiked = isLiked,
                routePoints = routePoints
        )
}
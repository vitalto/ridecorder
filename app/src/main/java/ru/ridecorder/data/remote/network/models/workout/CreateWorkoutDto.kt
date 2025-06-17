package ru.ridecorder.data.remote.network.models.workout

import ru.ridecorder.data.local.database.WorkoutEntity
import java.time.Instant

data class CreateWorkoutDto(
    val name: String? = "",
    val description: String? = "",
    val weight: Float? = null,
    val type: String? = "",
    val whoCanView: String? = "",
    val startTimestamp: Instant,
    val endTimestamp: Instant,
    val routePoints: List<RoutePointDto>? = emptyList()
)

fun WorkoutEntity.toCreateWorkoutDto(routePoints: List<RoutePointDto>? = null): CreateWorkoutDto {
    if(!this.isFinished) throw Exception("Для добавления в базу данных тренировка должна быть завершена")

    return CreateWorkoutDto(
        name = this.name,
        description = this.description,
        weight = this.weight,
        type = this.type,
        whoCanView = this.whoCanView,
        startTimestamp = this.startTimestamp!!,
        endTimestamp = this.endTimestamp!!,
        routePoints = routePoints
    )
}
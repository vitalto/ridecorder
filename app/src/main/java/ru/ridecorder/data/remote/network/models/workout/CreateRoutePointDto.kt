package ru.ridecorder.data.remote.network.models.workout

import ru.ridecorder.data.local.database.RoutePointEntity
import java.time.Instant

data class RoutePointDto(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val bearing: Float,
    val accuracy: Float,
    val isPause: Boolean,
    val timestamp: Instant,

    val provider: String?,
    val verticalAccuracyMeters: Float?,
    val bearingAccuracyDegrees: Float?,
    val speedAccuracyMetersPerSecond: Float?,
    val barometerAltitude: Float?,
    val heartRate: Int?
)

fun RoutePointEntity.toRoutePointDto(): RoutePointDto {
    return RoutePointDto(
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        speed = this.speed,
        bearing = this.bearing,
        accuracy = this.accuracy,
        isPause = this.isPause,
        timestamp = Instant.ofEpochMilli(this.timestamp), // Преобразуем millis в Instant

        provider = this.provider,
        verticalAccuracyMeters = this.verticalAccuracyMeters,
        bearingAccuracyDegrees = this.bearingAccuracyDegrees,
        speedAccuracyMetersPerSecond = this.speedAccuracyMetersPerSecond,
        barometerAltitude = this.barometerAltitude,
        heartRate = this.heartRate
    )
}

fun RoutePointDto.fromRoutePointDto(workoutId: Long): RoutePointEntity {
    return RoutePointEntity(
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        speed = this.speed,
        bearing = this.bearing,
        accuracy = this.accuracy,
        isPause = this.isPause,
        timestamp = this.timestamp.toEpochMilli(),
        workoutId = workoutId,

        provider = this.provider,
        verticalAccuracyMeters = this.verticalAccuracyMeters,
        bearingAccuracyDegrees = this.bearingAccuracyDegrees,
        speedAccuracyMetersPerSecond = this.speedAccuracyMetersPerSecond,
        barometerAltitude = this.barometerAltitude,
        heartRate = this.heartRate
    )
}


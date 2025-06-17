package ru.ridecorder.data.local.database

import android.location.Location
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)

data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,       // м/с
    var timestamp: Long,     // millis
    val isPause: Boolean,
    val altitude: Double,
    val bearing: Float,
    val provider: String?,
    val accuracy: Float,
    val verticalAccuracyMeters: Float?,
    val bearingAccuracyDegrees: Float?,
    val speedAccuracyMetersPerSecond: Float?,
    val barometerAltitude: Float?,
    val heartRate: Int?
) {

    fun distanceTo(other: RoutePointEntity): Float {
        val results = FloatArray(1) // В этот массив запишется результат
        Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
        return results[0] // Возвращаем расстояние в метрах
    }
    fun distanceTo(other: Location): Float {
        val results = FloatArray(1) // В этот массив запишется результат
        Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, results)
        return results[0] // Возвращаем расстояние в метрах
    }
}
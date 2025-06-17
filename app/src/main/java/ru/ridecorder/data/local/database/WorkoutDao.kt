package ru.ridecorder.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE isFinished = 1 ORDER BY startTimestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Insert
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    // Получение маршрута вместе с его точками
    @Transaction
    @Query("SELECT * FROM workouts WHERE isFinished = 1 ORDER BY startTimestamp DESC")
    fun getAllWorkoutsWithPoints(): Flow<List<WorkoutWithPoints>>

    @Transaction
    @Query("SELECT * FROM route_points WHERE workoutId = :workoutId")
    fun getAllRoutePoints(workoutId: Long): Flow<List<RoutePointEntity>>

    // Получаем незаконченный маршрут (если он есть)
    @Query("SELECT * FROM workouts WHERE isFinished = 0 LIMIT 1")
    suspend fun getUnfinishedWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithPoints(workoutId: Long): WorkoutWithPoints?

    // Удаляем маршрут по id
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)

    // Удаляем маршрут по id
    @Query("UPDATE workouts SET isDeleted = 1 WHERE id = :workoutId")
    suspend fun markWorkoutAsDeleted(workoutId: Long)

    // Отметить маршрут завершенным
    @Query("UPDATE workouts SET isFinished = 1 WHERE id = :workoutId")
    suspend fun markWorkoutAsFinished(workoutId: Long)

    @Query("SELECT * FROM workouts WHERE isFinished = 1 AND endTimestamp BETWEEN :start AND :end AND isFinished = 1 ORDER BY startTimestamp DESC")
    suspend fun getWorkoutsBetweenDates(start: Long, end: Long): List<WorkoutEntity>

    // Выбрать все workouts, требующие загрузки (serverId is NULL) или удаленные (isDeleted = true)
    @Query("SELECT * FROM workouts WHERE (serverId is NULL OR isDeleted = 1) AND isFinished = 1")
    suspend fun getPendingWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE serverId = :serverId")
    suspend fun getWorkoutByServerId(serverId: Int): WorkoutEntity?

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}

// Класс для отображения связи "один ко многим"
data class WorkoutWithPoints(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val points: List<RoutePointEntity>
)

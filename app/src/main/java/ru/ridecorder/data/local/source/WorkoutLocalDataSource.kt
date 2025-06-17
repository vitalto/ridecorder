package ru.ridecorder.data.local.source

import kotlinx.coroutines.flow.Flow
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutDao
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.local.database.WorkoutWithPoints
import javax.inject.Inject

class WorkoutLocalDataSource @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    fun getAllWorkouts(): Flow<List<WorkoutEntity>> {
        return workoutDao.getAllWorkouts()
    }

    suspend fun getPendingWorkouts(): List<WorkoutEntity> {
        return workoutDao.getPendingWorkouts()
    }

    suspend fun insertWorkout(workout: WorkoutEntity): Long {
        return workoutDao.insertWorkout(workout)
    }

    suspend fun updateWorkout(workout: WorkoutEntity) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun getWorkoutByServerId(serverId: Int): WorkoutEntity? {
        return workoutDao.getWorkoutByServerId(serverId)
    }
    suspend fun markWorkoutAsDeleted(workoutId: Long) {
        workoutDao.markWorkoutAsDeleted(workoutId)
    }
    suspend fun deleteWorkoutById(workoutId: Long) {
        workoutDao.deleteWorkoutById(workoutId)
    }
    suspend fun markWorkoutAsFinished(workoutId: Long) {
        workoutDao.markWorkoutAsFinished(workoutId)
    }
    suspend fun getWorkoutsBetweenDates(start: Long, end: Long): List<WorkoutEntity> {
        return workoutDao.getWorkoutsBetweenDates(start, end)
    }
    suspend fun getWorkoutWithPoints(workoutId: Long): WorkoutWithPoints? {
        return workoutDao.getWorkoutWithPoints(workoutId)
    }

    suspend fun insertRoutePoints(points: List<RoutePointEntity>) {
        return workoutDao.insertRoutePoints(points)
    }

    fun getAllWorkoutsWithPoints(): Flow<List<WorkoutWithPoints>> {
        return workoutDao.getAllWorkoutsWithPoints()
    }

    fun getAllRoutePoints(workoutId: Long): Flow<List<RoutePointEntity>> {
        return workoutDao.getAllRoutePoints(workoutId)
    }

    suspend fun getUnfinishedWorkout(): WorkoutEntity? {
        return workoutDao.getUnfinishedWorkout()
    }

    suspend fun getWorkoutById(workoutId: Long): WorkoutEntity? {
        return workoutDao.getWorkoutById(workoutId)
    }

    suspend fun deleteAllWorkouts() {
        workoutDao.deleteAllWorkouts()
    }


}

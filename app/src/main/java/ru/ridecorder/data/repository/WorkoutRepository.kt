package ru.ridecorder.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.ridecorder.data.local.database.WorkoutWithPoints
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.local.source.WorkoutLocalDataSource
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import ru.ridecorder.data.remote.network.models.workout.fromRoutePointDto
import ru.ridecorder.data.remote.network.models.workout.toRoutePointDto
import ru.ridecorder.data.remote.network.models.workout.toCreateWorkoutDto
import ru.ridecorder.data.remote.network.models.workout.toWorkoutEntity
import ru.ridecorder.data.remote.source.WorkoutRemoteDataSource
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val localDataSource: WorkoutLocalDataSource,
    private val remoteDataSource: WorkoutRemoteDataSource
) {
    suspend fun getUserWorkouts(userId: Int): ApiResponse<List<WorkoutDto>> {
        return remoteDataSource.fetchWorkouts(userId)
    }
    suspend fun getServerWorkoutWithPoints(serverWorkoutId: Int): ApiResponse<WorkoutDto> {
        return remoteDataSource.getWorkout(serverWorkoutId)
    }

    // Вставка новой тренировки
    suspend fun insertWorkoutLocally(workout: WorkoutEntity): Long {
        return localDataSource.insertWorkout(workout.copy(
            updatedAt = Instant.now(),
        ))
    }

    // Обновление тренировки
    suspend fun updateWorkoutLocally(workout: WorkoutEntity) {
        localDataSource.updateWorkout(
            workout.copy(
                updatedAt = Instant.now(),
            )
        )
    }

    // Вставка точек маршрута
    suspend fun insertRoutePoints(points: List<RoutePointEntity>) {
        localDataSource.insertRoutePoints(points)
    }

    // Получение всех завершенных тренировок
    fun getAllWorkouts(): Flow<List<WorkoutEntity>> {
        return localDataSource.getAllWorkouts()
    }

    // Получение всех тренировок с маршрутами
    fun getAllWorkoutsWithPoints(): Flow<List<WorkoutWithPoints>> {
        return localDataSource.getAllWorkoutsWithPoints()
    }

    // Получение всех точек маршрута по ID тренировки
    fun getAllRoutePoints(workoutId: Long): Flow<List<RoutePointEntity>> {
        return localDataSource.getAllRoutePoints(workoutId)
    }

    // Получение незавершенной тренировки (если есть)
    suspend fun getUnfinishedWorkout(): WorkoutEntity? {
        return localDataSource.getUnfinishedWorkout()
    }

    // Получение тренировки по ID
    suspend fun getWorkoutById(id: Long): WorkoutEntity? {
        return localDataSource.getWorkoutById(id)
    }

    // Получение тренировки с точками маршрута по ID
    suspend fun getWorkoutWithPoints(workoutId: Long): WorkoutWithPoints? {
        return localDataSource.getWorkoutWithPoints(workoutId)
    }

    // Удаление тренировки по ID
    suspend fun markWorkoutAsDeleted(workoutId: Long) {
        localDataSource.markWorkoutAsDeleted(workoutId)
    }

    suspend fun deleteWorkoutById(workoutId: Long) {
        localDataSource.deleteWorkoutById(workoutId)
    }

    // Отметить тренировку как завершенную
    suspend fun markWorkoutAsFinished(workoutId: Long) {
        localDataSource.markWorkoutAsFinished(workoutId)
    }

    // Получение тренировок за определенный период
    suspend fun getWorkoutsBetweenDates(start: Long, end: Long): List<WorkoutEntity> {
        return localDataSource.getWorkoutsBetweenDates(start, end)
    }

    suspend fun likeWorkout(workoutId: Int) : ApiResponse<Boolean>{
        return remoteDataSource.likeWorkout(workoutId)
    }
    suspend fun unlikeWorkout(workoutId: Int) : ApiResponse<Boolean>{
        return remoteDataSource.unlikeWorkout(workoutId)
    }
    /**
     * Синхронизация (Push + Pull):
     * 1) Отправляем локальные несинхронизированные изменения (pushLocalChanges).
     * 2) Загружаем с сервера новые/обновленные записи (pullRemoteChanges).
     */
    suspend fun sync(): Boolean {
        pushLocalChanges()
        return pullRemoteChanges()
    }

    /**
     * Шаг 1: Отправляем новые тренировки, удаляем удалённые
     */
    private suspend fun pushLocalChanges() {
        val pending = localDataSource.getPendingWorkouts()

        for (localWorkout in pending) {
            try {
                if (localWorkout.isDeleted) {
                    // Если на сервере есть serverId -> можно вызвать удаление,
                    if (localWorkout.serverId != null) {
                        // при успехе → удаляем физически из локальной БД (или храним "тумбстоун")
                        if(remoteDataSource.deleteWorkout(localWorkout.serverId).success){
                            localDataSource.deleteWorkoutById(localWorkout.id)
                        }
                    }
                    else
                    {
                        // Если serverId == null, значит тренировка никогда не существовала на сервере
                        // можно удалить её локально
                        localDataSource.deleteWorkoutById(localWorkout.id)
                    }

                } else {
                    // Новая запись (serverId == null)
                    if (localWorkout.serverId == null) {
                        val routePoints = localDataSource.getAllRoutePoints(localWorkout.id).first()
                        val routePointsDto = routePoints.map { it.toRoutePointDto() }
                        val response = remoteDataSource.createWorkout(
                            localWorkout.toCreateWorkoutDto(routePointsDto)
                        )
                        if (response.success) {
                            val newServerId = response.data!!
                            // Обновляем запись локально: ставим serverId, isSynced = true
                            localDataSource.updateWorkout(localWorkout.copy(
                                serverId = newServerId
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("pushLocalChanges", e.toString())
            }
        }
    }

    /**
     * Шаг 2: Получаем новые/изменённые тренировки с сервера и удаляем отсутствующие
     */
    private suspend fun pullRemoteChanges() : Boolean {
        try {
            val response = remoteDataSource.fetchWorkouts()
            if (response.success) {
                val remoteList = response.data ?: emptyList()
                val remoteIds = remoteList.map { it.id }.toSet()

                // Получаем все локальные тренировки
                val localWorkouts = localDataSource.getAllWorkouts().first()

                // Удаляем те, которых нет на сервере
                val workoutsToDelete = localWorkouts.filter { it.serverId != null && it.serverId !in remoteIds }
                for (workout in workoutsToDelete) {
                    localDataSource.deleteWorkoutById(workout.id)
                }

                for (workoutDto in remoteList) {
                    // Проверяем, есть ли локально запись с таким serverId
                    val existingLocal = localDataSource.getWorkoutByServerId(workoutDto.id)
                    val remoteUpdatedAt = workoutDto.updatedAt
                    if (existingLocal == null) {
                        // Создаём новую запись
                        val remoteDetailedWorkoutResponse = remoteDataSource.getWorkout(workoutDto.id)
                        if(remoteDetailedWorkoutResponse.success) {
                            val remoteDetailedWorkout = remoteDetailedWorkoutResponse.data!!
                            val remoteRoutePoints = remoteDetailedWorkout.routePoints!!

                            val newLocalWorkoutId = localDataSource
                                .insertWorkout(remoteDetailedWorkout.toWorkoutEntity())

                            localDataSource.insertRoutePoints(
                                remoteRoutePoints.map { it.fromRoutePointDto(newLocalWorkoutId) })
                        } else throw Exception("Не удалось получить тренировку с точками #${workoutDto.id}")
                    } else {
                        // Проверяем, у кого updatedAt новее
                        if (remoteUpdatedAt.isAfter(existingLocal.updatedAt)) {
                            // Сервер новее → обновляем локальную
                            localDataSource.updateWorkout(
                                workoutDto.toWorkoutEntity().copy(id = existingLocal.id)
                            )
                        } else if(existingLocal.updatedAt.isAfter(remoteUpdatedAt)) {
                            // Локальная новее → загружаем на сервер
                            if(existingLocal.serverId != null) {
                                remoteDataSource.updateWorkout(existingLocal.serverId,
                                    existingLocal.toCreateWorkoutDto())
                            }
                        } else {
                            localDataSource.updateWorkout(
                                existingLocal.copy(likesCount = workoutDto.likesCount)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("pullRemoteChanges", e.toString())
            return false
        }
        return true
    }

    suspend fun deleteLocalWorkouts() {
        localDataSource.deleteAllWorkouts()
    }
}

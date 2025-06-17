package ru.ridecorder.data.remote.source

import ru.ridecorder.data.remote.network.WorkoutApi
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.workout.CreateWorkoutDto
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import javax.inject.Inject

class WorkoutRemoteDataSource @Inject constructor(
    private val workoutApi: WorkoutApi
) {
    suspend fun fetchWorkouts(userId: Int? = null): ApiResponse<List<WorkoutDto>> {
        return workoutApi.getWorkouts(userId)
    }

    suspend fun createWorkout(request: CreateWorkoutDto): ApiResponse<Int> {
        return workoutApi.createWorkout(request)
    }

    suspend fun getWorkout(id: Int): ApiResponse<WorkoutDto> {
        return workoutApi.getWorkout(id)
    }

    suspend fun updateWorkout(id: Int, request: CreateWorkoutDto): ApiResponse<Boolean> {
        return workoutApi.updateWorkout(id, request)
    }

    suspend fun likeWorkout(id: Int): ApiResponse<Boolean> {
        return workoutApi.likeWorkout(id)
    }

    suspend fun unlikeWorkout(id: Int): ApiResponse<Boolean> {
        return workoutApi.unlikeWorkout(id)
    }

    suspend fun deleteWorkout(serverId: Int): ApiResponse<Boolean> {
        return workoutApi.deleteWorkout(serverId)
    }
}

package ru.ridecorder.data.remote.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.workout.CreateWorkoutDto
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto

interface WorkoutApi {
    @GET("api/Workouts")
    suspend fun getWorkouts(@Query("userId") userId: Int?): ApiResponse<List<WorkoutDto>>

    @POST("api/Workouts")
    suspend fun createWorkout(@Body request: CreateWorkoutDto): ApiResponse<Int>

    @GET("api/Workouts/{id}")
    suspend fun getWorkout(@Path("id") id: Int): ApiResponse<WorkoutDto>

    @PUT("api/Workouts/{id}")
    suspend fun updateWorkout(@Path("id") id: Int, @Body request: CreateWorkoutDto): ApiResponse<Boolean>

    @POST("api/Workouts/{id}/like")
    suspend fun likeWorkout(@Path("id") id: Int): ApiResponse<Boolean>

    @DELETE("api/Workouts/{id}/like")
    suspend fun unlikeWorkout(@Path("id") id: Int): ApiResponse<Boolean>

    @DELETE("api/Workouts/{id}")
    suspend fun deleteWorkout(@Path("id") id: Int): ApiResponse<Boolean>
}
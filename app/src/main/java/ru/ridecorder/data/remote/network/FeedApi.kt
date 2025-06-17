package ru.ridecorder.data.remote.network

import retrofit2.http.GET
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto

interface FeedApi {
    @GET("api/Feed")
    suspend fun getFeed(): ApiResponse<List<WorkoutDto>>
}
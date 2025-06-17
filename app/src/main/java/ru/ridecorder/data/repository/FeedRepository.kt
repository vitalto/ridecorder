package ru.ridecorder.data.repository

import ru.ridecorder.data.remote.network.FeedApi
import ru.ridecorder.data.remote.network.models.ApiResponse
import ru.ridecorder.data.remote.network.models.workout.WorkoutDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(private val feedApi: FeedApi) {
    suspend fun getFeed(): ApiResponse<List<WorkoutDto>> = feedApi.getFeed()
}

package ru.ridecorder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.local.source.WorkoutLocalDataSource
import ru.ridecorder.data.remote.network.AuthApi
import ru.ridecorder.data.remote.network.FeedApi
import ru.ridecorder.data.remote.network.ProfileApi
import ru.ridecorder.data.remote.source.WorkoutRemoteDataSource
import ru.ridecorder.data.repository.AuthRepository
import ru.ridecorder.data.repository.FeedRepository
import ru.ridecorder.data.repository.ProfileRepository
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(authApi: AuthApi): AuthRepository {
        return AuthRepository(authApi)
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        localDataSource: WorkoutLocalDataSource,
        remoteDataSource: WorkoutRemoteDataSource
    ): WorkoutRepository {
        return WorkoutRepository(localDataSource, remoteDataSource)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        profileApi: ProfileApi,
        userDataStore: UserDataStore
    ): ProfileRepository {
        return ProfileRepository(profileApi, userDataStore)
    }

    @Provides
    @Singleton
    fun provideFeedRepository(
        feedApi: FeedApi,
    ): FeedRepository {
        return FeedRepository(feedApi)
    }

}

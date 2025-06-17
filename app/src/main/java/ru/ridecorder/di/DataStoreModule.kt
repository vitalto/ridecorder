package ru.ridecorder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.ridecorder.data.local.preferences.SettingsDataStore
import ru.ridecorder.data.local.preferences.TokenDataStore
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.data.local.source.WorkoutLocalDataSource
import ru.ridecorder.data.remote.network.AuthApi
import ru.ridecorder.data.remote.source.WorkoutRemoteDataSource
import ru.ridecorder.data.repository.AuthRepository
import ru.ridecorder.data.repository.WorkoutRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTokenDataStore(@ApplicationContext context: Context): TokenDataStore {
        return TokenDataStore(context)
    }

    @Provides
    @Singleton
    fun provideUserDataStore(@ApplicationContext context: Context): UserDataStore {
        return UserDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}

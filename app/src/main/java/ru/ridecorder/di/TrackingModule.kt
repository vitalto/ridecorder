package ru.ridecorder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.ridecorder.domain.tracking.RoutePointsManager
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class) // Гарантирует, что RoutePointsManager живёт столько же, сколько приложение
object TrackingModule {

    @Provides
    @Singleton
    fun provideRoutePointsManager(): RoutePointsManager {
        return RoutePointsManager()
    }
}
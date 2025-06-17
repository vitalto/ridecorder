package ru.ridecorder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.ridecorder.data.local.GpxService

@Module
@InstallIn(ViewModelComponent::class)
object GpxModule {

    @Provides
    fun provideGpxService(@ApplicationContext context: Context): GpxService {
        return GpxService(context)
    }
}

package ru.ridecorder.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.ridecorder.config.AppConfig
import ru.ridecorder.data.local.preferences.TokenDataStore
import ru.ridecorder.data.remote.AuthInterceptor
import ru.ridecorder.data.remote.InstantAdapter
import ru.ridecorder.data.remote.network.AuthApi
import ru.ridecorder.data.remote.network.FeedApi
import ru.ridecorder.data.remote.network.ProfileApi
import ru.ridecorder.data.remote.network.WorkoutApi
import java.time.Instant
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenDataStore: TokenDataStore): AuthInterceptor {
        return AuthInterceptor(tokenDataStore)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Добавляем интерсептор
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantAdapter()) // Регистрация адаптера
            .create()

        return Retrofit.Builder()
            .baseUrl(AppConfig.serverUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWorkoutApi(retrofit: Retrofit): WorkoutApi {
        return retrofit.create(WorkoutApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi {
        return retrofit.create(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedApi(retrofit: Retrofit): FeedApi {
        return retrofit.create(FeedApi::class.java)
    }
}

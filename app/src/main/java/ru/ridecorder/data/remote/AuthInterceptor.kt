package ru.ridecorder.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import ru.ridecorder.data.local.preferences.TokenDataStore
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.getToken() } // Получаем токен синхронно
        val request = chain.request().newBuilder()

        token?.let {
            request.addHeader("Authorization", "Bearer $it") // Добавляем заголовок
        }

        return chain.proceed(request.build())
    }
}
package ru.ridecorder

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import ru.ridecorder.config.AppConfig

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Установите API-ключ перед инициализацией MapKit
        MapKitFactory.setApiKey(AppConfig.mapsApiKey)
        MapKitFactory.initialize(this)
    }
}
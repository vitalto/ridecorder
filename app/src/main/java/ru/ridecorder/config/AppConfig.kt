package ru.ridecorder.config

import ru.ridecorder.BuildConfig

object AppConfig {
    val serverUrl: String get() = BuildConfig.BACKEND_URL
    val mapsApiKey: String get() = BuildConfig.MAPS_API_KEY
}
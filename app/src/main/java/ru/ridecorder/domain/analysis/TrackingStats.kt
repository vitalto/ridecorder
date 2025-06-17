package ru.ridecorder.domain.analysis

data class TrackingStats(
    val currentSpeed: Float = 0f, // м/с
    val averageSpeed: Float = 0f, // м/с
    val distance: Float = 0f,
    val activeDuration: Long = 0,      // milliseconds
    val pauseDuration: Long = 0 // milliseconds
)
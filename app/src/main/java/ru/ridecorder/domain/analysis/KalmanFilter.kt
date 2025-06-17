package ru.ridecorder.domain.analysis

// Фильтр Калмана для стабилизации высоты
class KalmanFilter(private val processNoise: Double = 0.001, private val measurementNoise: Double = 5.0) {
    private var estimate = 0.0
    private var error = 1.0

    fun filter(measurement: Double): Double {
        // Предсказание
        val predictedEstimate = estimate
        val predictedError = error + processNoise

        // Коррекция
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate)
        error = (1 - kalmanGain) * predictedError

        return estimate
    }
}
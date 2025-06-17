package ru.ridecorder.domain.analysis

/**
 * Основные метрики, которые мы получаем после анализа маршрута.
 * При желании можно расширять и добавлять все интересующие показатели.
 */
data class RouteAnalytics(
    val totalDistanceMeters: Float,      // Общая дистанция в метрах
    val totalDurationSeconds: Float,     // Общая длительность в секундах (без учёта пауз, если надо)
    val averageSpeedMps: Float,         // Средняя скорость (м/с)
    val maxSpeedMps: Float,             // Максимальная скорость (м/с)
    val minSpeedMps: Float,             // Минимальная скорость (м/с)
    val altitudeRange: Double,          // Разница между максимальной и минимальной высотой (м)
    val totalAltitudeGain: Double,      // Суммарный набор высоты (м)
    val totalAltitudeLoss: Double,      // Суммарный спуск по высоте (м)
    val startAltitude: Double,          // Высота первой точки
    val endAltitude: Double,            // Высота последней точки
    val maxAltitude: Double,            // Максимальная высота
    val minAltitude: Double,            // Минимальная высота
    val averagePaceSecPerKm: Float,     // Средний темп (сек / км), если нужно
    val maxGradientPercent: Float,      // Максимальный градиент подъёма/спуска (%) среди всех сегментов

    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val avgHeartRage: Int?,

    val caloriesBurned: Double
)
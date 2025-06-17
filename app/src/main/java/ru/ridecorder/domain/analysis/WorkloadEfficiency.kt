package ru.ridecorder.domain.analysis

import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.domain.analysis.RouteStatsCalculator.cumulativeDistance
import ru.ridecorder.domain.analysis.RouteStatsCalculator.smoothSpeedHeartRate

object WorkloadEfficiency {
    // --- параметры -----------------------------------
    private const val MIN_SPEED_MS  = 0.55f   // 2 км/ч: всё, что ниже – считаем «стоя»
    private const val MAX_RATIO     = 25f     // отсечём заведомый мусор ≥ 25 BPM/(км·ч)
    private const val SMOOTH_WINDOW = 7       // ширина окна скользящего среднего
// ----------------------------------------------------------------------------

    /**
     * График «эффективность» по времени от старта (минуты).
     * Y = HR (BPM) / speed (км/ч), сглаженный и без выбросов.
     */
    fun workloadEfficiencyOverTime(points: List<RoutePointEntity>): List<GraphDataPoint> {
        val cleaned = points.cleanPoints()
        if (cleaned.isEmpty()) return emptyList()

        val startTs   = cleaned.first().timestamp
        val ratioList = cleaned
            .map { it.heartRate!! / (it.speed * 3.6f) }          // HR / km·h⁻¹
            .map { it.coerceIn(0f, MAX_RATIO) }                  // обрезаем шум
            .movingAverage(SMOOTH_WINDOW)                        // сглаживаем

        return cleaned.mapIndexed { idx, pt ->
            GraphDataPoint(
                x = (pt.timestamp - startTs) / 1000f / 60f,            // секунд от старта
                y = ratioList[idx]
            )
        }
    }

    /**
     * Тот же график, но по накопленной дистанции (километры).
     */
    fun workloadEfficiencyOverDistance(points: List<RoutePointEntity>): List<GraphDataPoint> {
        val cleaned = points.cleanPoints()
        if (cleaned.isEmpty()) return emptyList()

        val distMeters = cumulativeDistance(cleaned)
        val ratioList  = cleaned
            .map { it.heartRate!! / (it.speed * 3.6f) }
            .map { it.coerceIn(0f, MAX_RATIO) }
            .movingAverage(SMOOTH_WINDOW)

        return List(cleaned.size) { idx ->
            GraphDataPoint(
                x = distMeters[idx] / 1000f,                     // км
                y = ratioList[idx]
            )
        }
    }

    /* ------------------------ вспомогательные утилиты ------------------------ */

    /** Отфильтровывает «плохие» точки и применяет ваше сглаживание HR/скорости. */
    private fun List<RoutePointEntity>.cleanPoints(): List<RoutePointEntity> =
        smoothSpeedHeartRate(this)                               // ваша функция
            .filter { p ->
                val hrOk    = p.heartRate != null && p.heartRate > 30
                val speedOk = p.speed.isFinite() && p.speed > MIN_SPEED_MS
                hrOk && speedOk
            }

    /** Простое скользящее среднее O(n), без доп.памяти. */
    private fun List<Float>.movingAverage(window: Int): List<Float> {
        if (window <= 1 || size <= 1) return this
        val out = MutableList(size) { 0f }
        var acc = 0f
        for (i in indices) {
            acc += this[i]
            if (i >= window) acc -= this[i - window]
            out[i] = acc / minOf(i + 1, window)
        }
        return out
    }
}
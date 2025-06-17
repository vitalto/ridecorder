package ru.ridecorder.domain.analysis

import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutEntity

/**
 * Класс для расчёта калорий, сожжённых во время велотренировки.
 * Использует данные из WorkoutDto и, при наличии, учитывает:
 * - вес (weight)
 * - пол (gender)
 * - среднюю скорость (averageSpeed)
 * - суммарный набор высоты (routePoints)
 * - средний пульс (heartRate), вычисляется при наличии данных в routePoints
 *
 * Все расчёты являются примерными и могут нуждаться в дополнительной калибровке
 * под конкретные данные и пользователей.
 */
object CalorieCalculator {

    /**
     * Основной метод для расчёта калорий.
     *
     * @param workout данные о тренировке (WorkoutDto)
     * @param routePoints список точек маршрута (RoutePointDto)
     * @param gender пол (например, "male", "female" или null, если не указан)
     * @return примерное количество сожжённых калорий
     */
    fun calculateCalories(
        workout: WorkoutEntity,
        routePoints: List<RoutePointEntity>?,
        gender: String? = null
    ): Double {
        // 1) Получаем вес, если нет — используем условно 70 кг
        val weightKg = workout.weight ?: 70f

        // 2) Переводим длительность тренировки из миллисекунд в часы
        val durationHours = (workout.duration ?: 0) / 1000 / 3600.0

        // 3) Если маршрутных точек нет, мы не сможем рассчитать часть параметров,
        //    но продолжим расчёт с тем, что есть
        val averageHeartRate = computeAverageHeartRate(routePoints)
        val totalAltitudeGain = computePositiveAltitudeGain(routePoints)

        // 4) Рассчитываем «базовые» калории по MET (зависит от скорости, пульса, пола)
        val baseCalories = calculateBaseCaloriesByMET(
            weightKg = weightKg,
            averageSpeedKmh = workout.averageSpeed?.times(3.6) ?: 0.0,
            durationHours = durationHours,
            averageHeartRate = averageHeartRate,
            gender = gender
        )

        // 5) Рассчитываем дополнительную часть калорий за набор высоты
        val altitudeCalories = calculateAltitudeCalories(weightKg, totalAltitudeGain)

        // Итоговое количество сожжённых калорий
        return baseCalories + altitudeCalories
    }

    /**
     * Расчёт «базовой» части калорий на основе MET.
     *
     * - MET зависит от средней скорости (averageSpeedKmh).
     * - При наличии среднего пульса (averageHeartRate) корректируем MET (упрощённая схема).
     * - Можно также учесть пол (gender), чтобы скорректировать результат.
     */
    private fun calculateBaseCaloriesByMET(
        weightKg: Float,
        averageSpeedKmh: Double,
        durationHours: Double,
        averageHeartRate: Double?,
        gender: String?
    ): Double {
        // Определяем базовый MET согласно Compendium of Physical Activities
        // https://cdn-links.lww.com/permalink/mss/a/mss_43_8_2011_06_13_ainsworth_202093_sdc1.pdf#:~:text=01019%205,19%20mph
        // Примерные диапазоны:
        // - < 16 км/ч – очень лёгкая езда (~4.0 MET)
        // - 16–19 км/ч – лёгкая/умеренная езда (~6.0 MET)
        // - 19–22 км/ч – умеренная езда (~8.0 MET)
        // - 22–26 км/ч – энергичная езда (~10.0 MET)
        // - > 26 км/ч – очень интенсивная езда (гонка, ~15.8 MET)
        val baseMet = when {
            averageSpeedKmh < 16 -> 4.0
            averageSpeedKmh < 19 -> 6.0
            averageSpeedKmh < 22 -> 8.0
            averageSpeedKmh < 26 -> 10.0
            else -> 15.8
        }

        // Коррекция MET с учётом среднего пульса
        // отклонение реального пульса от ожидаемого (например, 140 уд/мин для умеренной нагрузки)
        val expectedHR = 140.0  // ожидаемый пульс для умеренной интенсивности
        val hrFactor = averageHeartRate?.let { hr ->
            // Если пульс выше ожидаемого – увеличиваем MET, если ниже – уменьшаем.
            // Ограничиваем поправку в пределах ±20% (коэффициент от 0.8 до 1.2).
            (1 + (hr - expectedHR) / 100.0).coerceIn(0.8, 1.2)
        } ?: 1.0

        val metWithHR = baseMet * hrFactor

        // Поправка на пол: согласно исследованиям, женщины могут иметь слегка меньшую энергетическую затрату при одинаковой нагрузке.
        val genderFactor = when (gender?.lowercase()) {
            "male" -> 1.0
            "female" -> 0.95
            else -> 1.0
        }

        // Итоговая формула расчёта калорий:
        // ккал = MET * вес (кг) * время (ч)
        return metWithHR * weightKg * durationHours * genderFactor
    }

    /**
     * Расчёт калорий за счёт набора высоты.
     * Основано на физической работе против силы тяжести: W = m * g * h.
     * Затем переводим из Дж (Джоулей) в ккал.
     */
    private fun calculateAltitudeCalories(weightKg: Float, altitudeGainMeters: Double): Double {
        val gravity = 9.81
        val joules = weightKg * gravity * altitudeGainMeters
        // 1 ккал ~ 4184 Дж
        return joules / 4184.0
    }

    /**
     * Суммируем только «позитивные» приращения высоты (разницу между соседними точками, если она > 0).
     */
    private fun computePositiveAltitudeGain(routePoints: List<RoutePointEntity>?): Double {
        if (routePoints.isNullOrEmpty()) return 0.0

        var totalGain = 0.0
        for (i in 1 until routePoints.size) {
            val prevAlt = routePoints[i - 1].altitude
            val currentAlt = routePoints[i].altitude
            val diff = currentAlt - prevAlt
            if (diff > 0) {
                totalGain += diff
            }
        }
        return totalGain
    }

    /**
     * Рассчитываем средний пульс по всем точкам, где heartRate > 0.
     */
    private fun computeAverageHeartRate(routePoints: List<RoutePointEntity>?): Double? {
        if (routePoints.isNullOrEmpty()) return null

        var sumHr = 0
        var countHr = 0
        for (point in routePoints) {
            val hr = point.heartRate
            if (hr != null && hr > 0) {
                sumHr += hr
                countHr++
            }
        }
        if (countHr == 0) return null
        return sumHr.toDouble() / countHr
    }
}

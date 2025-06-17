package ru.ridecorder.domain.analysis

import ru.ridecorder.data.local.database.RoutePointEntity

object RecoveryPhaseAnalyzer {
    /* ---------- ПАРАМЕТРЫ АЛГОРИТМА ---------- */
    private const val MEDIAN_WIN = 1               // сглаживание HR, точек
    private const val MIN_PEAK_DIFF = 4            // пик выше соседей ≥ 5 BPM
    private const val MIN_PHASE_DUR_SEC = 10       // фаза ≥ 10 с
    private const val MIN_DELTA_HR = 8             // HR должен упасть ≥ 8 BPM
    private const val HRR_WINDOW_SEC = 60          // окно для HRR‑60
    private const val STEP_SEC = 10                // промежуточные точки на графике
    private const val SPEED_MIN = 0.05f            // реалистичный диапазон
    private const val SPEED_MAX = 1.2f

    /**
     * Возвращает список (X, Y) для графика скорости восстановления.
     *
     * Алгоритм:
     * 1. Сглаживаем пульс медианным фильтром (борьба с шумом).
     * 2. Ищем «строгие» пики (выше обеих соседних точек и разница ≥ MIN_PEAK_DIFF).
     * 3. Идём вперёд, пока пульс строго не растёт; фиксируем конец фазы.
     * 4. Фильтруем короткие или «пустые» фазы (длительность ↘, ∆HR ↘).
     * 5. Для каждой фазы считаем HRR‑60  → среднюю скорость (∆HR / 60 с),
     *    плюс выдаём несколько промежуточных скоростей каждые STEP_SEC.
     */
    fun recoveryPhases(raw: List<RoutePointEntity>): List<GraphDataPoint> {
        /* 0. Предварительная проверка */
        val hrPoints = raw
            .filter { it.heartRate != null && it.heartRate > 0 }
            .sortedBy { it.timestamp }
        if (hrPoints.size < 3) return emptyList()

        /* 1. Сглаживаем HR */
        val smooth = medianSmooth(hrPoints.map { it.heartRate!! }, MEDIAN_WIN)

        val startTrackTime = hrPoints.first().timestamp
        val result = mutableListOf<GraphDataPoint>()
        var i = 1                                    // индекс «центральной» точки окна

        while (i < smooth.size - 1) {
            val prevHR = smooth[i - 1]
            val currHR = smooth[i]
            val nextHR = smooth[i + 1]

            /* 2. Детектор строгого пика */
            val isPeak = currHR > prevHR + MIN_PEAK_DIFF &&
                    currHR >= nextHR + MIN_PEAK_DIFF
            if (!isPeak) {
                i++; continue
            }

            val peakTime = hrPoints[i].timestamp
            var j = i + 1

            /* 3. Спускаемся, пока HR не растёт */
            while (j < smooth.size && smooth[j] <= smooth[j - 1]) j++
            val endIdx = j - 1
            val phaseDurSec = (hrPoints[endIdx].timestamp - peakTime) / 1000.0

            /* 4. Фильтр «плохих» фаз */
            val deltaHr = currHR - smooth[endIdx]
            if (phaseDurSec < MIN_PHASE_DUR_SEC || deltaHr < MIN_DELTA_HR) {
                i++; continue
            }

            /* 5‑a. HRR‑60 */
            val hrrTargetTime = peakTime + HRR_WINDOW_SEC * 1000
            val hrrTime = minOf(hrrTargetTime, hrPoints[endIdx].timestamp)
            val hrrHR = interpolateHR(hrPoints, hrrTime) ?: smooth[endIdx].toDouble()
            val hrrDelta = currHR - hrrHR
            val hrrSpeed = (hrrDelta / ( (hrrTime - peakTime) / 1000.0 )).toFloat()

            addPointIfOk(result, startTrackTime, hrrTime, hrrSpeed)

            /* 5‑b. Промежуточные точки каждые STEP_SEC */
            var t = peakTime + STEP_SEC * 1000
            while (t < hrrTime) {
                val interpHR = interpolateHR(hrPoints, t) ?: break
                val speed = ((currHR - interpHR) / ((t - peakTime) / 1000.0)).toFloat()
                addPointIfOk(result, startTrackTime, t, speed)
                t += STEP_SEC * 1000
            }

            i = endIdx + 1            // прыгаем к следующему кандидату
        }
        return result.sortedBy { it.x }
    }

    /* ---------- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ---------- */

    /**
     * Медианный фильтр, нечётное окно `win`.
     */
    private fun medianSmooth(src: List<Int>, win: Int): List<Int> {
        if (win <= 1) return src
        val half = win / 2
        return src.indices.map { idx ->
            val from = maxOf(0, idx - half)
            val to = minOf(src.lastIndex, idx + half)
            src.subList(from, to + 1).sorted()[ (to - from) / 2 ]
        }
    }

    /**
     * Добавляем точку, если скорость в реалистичных пределах.
     */
    private fun addPointIfOk(
        acc: MutableList<GraphDataPoint>,
        startTs: Long,
        timeMs: Long,
        speed: Float
    ) {
        if (speed in SPEED_MIN..SPEED_MAX) {
            acc.add(
                GraphDataPoint(
                    x = ((timeMs - startTs) / 1000f) / 60f,  // мин с начала трека
                    y = speed
                )
            )
        }
    }

    /**
     * Линейная интерполяция HR в момент `timeMs`.
     * Работает по «сырым» точкам (без сглаживания!), чтобы не терять точность.
     */
    private fun interpolateHR(points: List<RoutePointEntity>, timeMs: Long): Double? {
        val idx = points.indexOfLast { it.timestamp <= timeMs }
        if (idx < 0) return null
        if (idx >= points.lastIndex) return points.last().heartRate?.toDouble()

        val p1 = points[idx]; val p2 = points[idx + 1]
        val (t1, t2) = p1.timestamp to p2.timestamp
        val (hr1, hr2) = p1.heartRate!!.toDouble() to p2.heartRate!!.toDouble()

        if (t2 == t1) return hr1               // дубликаты по времени
        val ratio = (timeMs - t1).toDouble() / (t2 - t1)
        return hr1 + (hr2 - hr1) * ratio
    }
}
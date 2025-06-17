package ru.ridecorder.domain.analysis


import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutEntity
import kotlin.math.abs

object RouteStatsCalculator {

    /**
     * Главная функция, которую вызываем для получения комплексного анализа.
     * Возвращает [RouteAnalytics] со всеми нужными метриками.
     */
    fun analyze(workout: WorkoutEntity, points: List<RoutePointEntity>, gender: String?): RouteAnalytics {
        // Если точек нет — возвращаем пустой результат
        if (points.isEmpty()) {
            return RouteAnalytics(
                totalDistanceMeters = 0f,
                totalDurationSeconds = 0f,
                averageSpeedMps = 0f,
                maxSpeedMps = 0f,
                minSpeedMps = 0f,
                altitudeRange = 0.0,
                totalAltitudeGain = 0.0,
                totalAltitudeLoss = 0.0,
                startAltitude = 0.0,
                endAltitude = 0.0,
                maxAltitude = 0.0,
                minAltitude = 0.0,
                averagePaceSecPerKm = 0f,
                maxGradientPercent = 0f,
                minHeartRate = null,
                maxHeartRate = null,
                avgHeartRage = null,
                caloriesBurned = 0.0
            )
        }
        
        // Отбираем список точек, которые не являются паузами.
        val activePoints = points.filter { !it.isPause }
        if (activePoints.isEmpty()) {
            // Все точки — паузы, значит дистанция и скорость нулевые
            return RouteAnalytics(
                totalDistanceMeters = 0f,
                totalDurationSeconds = 0f,
                averageSpeedMps = 0f,
                maxSpeedMps = 0f,
                minSpeedMps = 0f,
                altitudeRange = 0.0,
                totalAltitudeGain = 0.0,
                totalAltitudeLoss = 0.0,
                startAltitude = 0.0,
                endAltitude = 0.0,
                maxAltitude = 0.0,
                minAltitude = 0.0,
                averagePaceSecPerKm = 0f,
                maxGradientPercent = 0f,
                minHeartRate = null,
                maxHeartRate = null,
                avgHeartRage = null,
                caloriesBurned = 0.0
            )
        }

        // === 1. Высотные показатели ===
        val minAlt = activePoints.minOf { it.altitude }
        val maxAlt = activePoints.maxOf { it.altitude }
        val altitudeRange = maxAlt - minAlt
        val startAltitude = activePoints.first().altitude
        val endAltitude = activePoints.last().altitude

        var altitudeGain = 0.0
        var altitudeLoss = 0.0

        // Считаем набор/спуск высоты
        for (i in 1 until activePoints.size) {
            if(activePoints[i].isPause || activePoints[i - 1].isPause) continue

            val altDiff = activePoints[i].altitude - activePoints[i - 1].altitude
            if (altDiff > 0) {
                altitudeGain += altDiff
            } else {
                altitudeLoss += abs(altDiff)
            }
        }

        // Расчёт градиента
        val maxGradient = calculateMaxGradient(activePoints)

        // === 2. Расчёт дистанции ===
        val totalDistance = calculateTotalDistance(activePoints)

        // === 3. Расчёт общего времени (секунды) ===
        val (_, activeDurationMs) = calculateRouteDuration(points)

        // === 4. Скорости ===
        val maxSpeedMps = activePoints.maxOf { it.speed }
        val minSpeedMps = activePoints.minOf { it.speed }

        val averageSpeedMps = if (activeDurationMs > 0) {
            // Переводим duration из миллисекунд в секунды, distance – в метрах,
            val seconds = activeDurationMs / 1000.0
            (totalDistance / seconds).toFloat() // м/с
        } else 0f

        // === 5. Темп (сек/км) ===
        val averagePace = if (totalDistance > 0) {
            activeDurationMs / 1000 / (totalDistance / 1000f) // секунды на км
        } else 0f

        val heartRateList = activePoints
            .filter { it.heartRate != null && it.heartRate > 0 }
            .map { it.heartRate!! }

        val minHeartRate = heartRateList.minOrNull()
        val maxHeartRate = heartRateList.maxOrNull()
        val avgHeartRate = if (heartRateList.isNotEmpty()) heartRateList.average() else 0.0

        val caloriesBurned = CalorieCalculator.calculateCalories(workout, activePoints, gender)
        // Формируем итоговый объект со всеми показателями
        return RouteAnalytics(
            totalDistanceMeters = totalDistance,
            totalDurationSeconds = activeDurationMs / 1000f,
            averageSpeedMps = averageSpeedMps,
            maxSpeedMps = maxSpeedMps,
            minSpeedMps = minSpeedMps,
            altitudeRange = altitudeRange,
            totalAltitudeGain = altitudeGain,
            totalAltitudeLoss = altitudeLoss,
            startAltitude = startAltitude,
            endAltitude = endAltitude,
            maxAltitude = maxAlt,
            minAltitude = minAlt,
            averagePaceSecPerKm = averagePace,
            maxGradientPercent = maxGradient.third,
            minHeartRate = minHeartRate,
            maxHeartRate = maxHeartRate,
            avgHeartRage = avgHeartRate.toInt(),
            caloriesBurned = caloriesBurned
        )
    }

    // ==============================
    // Методы для формирования данных графиков
    // ==============================

    /**
     * График "График изменения скорости со временем" (speed vs. time).
     * Возвращает список точек:
     * x = (время в секундах с момента старта)
     * y = скорость (км/ч)
     */
    fun speedOverTime(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = smoothSpeedParameter(points)

        val startTimestamp = filteredPoints.first().timestamp
        return filteredPoints.map { rp ->
            val timeSec = (rp.timestamp - startTimestamp) / 1000f
            GraphDataPoint(x = timeSec, y = rp.speed * 3.6f)
        }
    }

    /**
     * График "График изменения скорости в зависимости от дистанции" (speed vs. distance).
     * Возвращает список точек:
     * x = пройденная дистанция (м) от начала
     * y = скорость (км/ч)
     */
    fun speedOverDistance(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = smoothSpeedParameter(points)

        val distances = cumulativeDistance(filteredPoints)
        return filteredPoints.mapIndexed { index, rp ->
            GraphDataPoint(x = distances[index], y = rp.speed * 3.6f)
        }
    }

    /**
     * График "График изменения высоты со временем" (altitude vs. time).
     * Возвращает список точек:
     * x = (время в секундах от начала)
     * y = высота (м)
     */
    fun altitudeOverTime(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = filterAltitudesGpsOnly(points)
        val baseAltitude = filteredPoints.first().altitude.toFloat() // базовая высота первой точки
        val startTimestamp = filteredPoints.first().timestamp
        return filteredPoints.map { rp ->
            val timeSec = (rp.timestamp - startTimestamp) / 1000f
            GraphDataPoint(x = timeSec, y = rp.altitude.toFloat() - baseAltitude)
        }
    }

    /**
     * График "График изменения высоты в зависимости от дистанции" (altitude vs. distance).
     * Возвращает список точек:
     * x = пройденная дистанция (м) от начала
     * y = высота (м)
     */
    fun altitudeOverDistance(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = filterAltitudesGpsOnly(points)
        val baseAltitude = filteredPoints.first().altitude.toFloat() // базовая высота первой точки
        val distances = cumulativeDistance(points)
        return filteredPoints.mapIndexed { index, fp ->
            GraphDataPoint(x = distances[index], y = fp.altitude.toFloat() - baseAltitude)
        }
    }

    /**
     * График "График изменения пульса со временем" (heart rate vs. time).
     * Возвращает список точек:
     * x = (время в секундах с момента старта)
     * y = скорость (км/ч)
     */
    fun heartRateOverTime(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = points.filter { it.heartRate != null && it.heartRate > 0 }

        val startTimestamp = points.first().timestamp
        return filteredPoints.map { rp ->
            val timeSec = (rp.timestamp - startTimestamp) / 1000f
            GraphDataPoint(x = timeSec, y = rp.heartRate!!.toFloat())
        }
    }

    /**
     * График "График изменения пульса в зависимости от дистанции" (heart rate vs. distance).
     * Возвращает список точек:
     * x = пройденная дистанция (м) от начала
     * y = скорость (км/ч)
     */
    fun heartRateOverDistance(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()
        val filteredPoints = points.filter { it.heartRate != null && it.heartRate > 0 }

        val distances = cumulativeDistance(filteredPoints)
        return filteredPoints.mapIndexed { index, rp ->
            GraphDataPoint(x = distances[index], y = rp.heartRate!!.toFloat())
        }
    }

    /**
     * График "Кумулятивная нагрузка".
     * Ось X: время (минуты от старта).
     * Ось Y: накопленный интеграл нагрузки:
     *   dLoad = (средний пульс * средняя скорость (км/ч) * средняя высота (км)) * dt (мин)
     */
    fun cumulativeLoadOverTime(points: List<RoutePointEntity>): List<GraphDataPoint> {
        if (points.size < 2) return emptyList()

        var cumulativeLoad = 0.0
        val graphPoints = mutableListOf<GraphDataPoint>()
        val startTimestamp = points.first().timestamp

        for (i in 0 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]

            // Проверяем, что данные валидны
            if (current.heartRate != null && next.heartRate != null
                && current.speed > 0 && next.speed > 0
            ) {
                val avgHR = (current.heartRate + next.heartRate) / 2.0
                val avgSpeedKmh = ((current.speed + next.speed) / 2.0) * 3.6
                // altitude считается в метрах, поэтому переводим в км
                val avgAltitudeKm = ((current.altitude + next.altitude) / 2.0) / 1000.0
                // Δt в минутах
                val dtMin = (next.timestamp - current.timestamp) / 60000.0
                val segmentLoad = avgHR * avgSpeedKmh * avgAltitudeKm * dtMin
                cumulativeLoad += segmentLoad
            }

            // Записываем точку на время конца текущего сегмента
            val timeMin = (points[i + 1].timestamp - startTimestamp) / 60000f
            graphPoints.add(
                GraphDataPoint(
                    x = timeMin,
                    y = cumulativeLoad.toFloat()
                )
            )
        }
        return graphPoints
    }

    /**
     * Главный метод, который принимает список точек (routePoints)
     * и возвращает готовую статистику TrackingStats.
     */
    fun calculateTrackingStats(routePoints: List<RoutePointEntity>): TrackingStats {
        if (routePoints.isEmpty()) return TrackingStats()

        val distance = calculateTotalDistance(routePoints)
        val (pauseDuration, activeDuration) = calculateRouteDuration(routePoints)

        val averageSpeed = if (activeDuration > 0) {
            // Переводим duration из миллисекунд в секунды, distance – в метрах,
            val seconds = activeDuration / 1000.0
            (distance / seconds).toFloat() // м/с
        } else 0f

        val currentSpeed = routePoints.last().speed // м/с

        return TrackingStats(
            currentSpeed = currentSpeed,
            averageSpeed = averageSpeed,
            distance = distance, // в метрах
            activeDuration = activeDuration,
            pauseDuration = pauseDuration
        )
    }

    private fun calculateRouteDuration(routePoints: List<RoutePointEntity>): Pair<Long, Long> {
        val totalDuration = routePoints.last().timestamp - routePoints.first().timestamp

        // Считаем суммарное время пауз
        val pauseDuration = routePoints
            .zipWithNext() // Создаём пары (prev, next)
            .filter { it.second.isPause } // Оставляем только те пары, где следующая точка — пауза
            .sumOf { (prev, next) -> next.timestamp - prev.timestamp } // Вычисляем разницу и суммируем

        val activeDuration = totalDuration - pauseDuration
        return Pair(pauseDuration, activeDuration)
    }

    /**
     * Подсчитывает суммарную дистанцию, складывая расстояния между соседними точками.
     * Исключая точки, добавленные на паузе
     */
    private fun calculateTotalDistance(routePoints: List<RoutePointEntity>): Float {
        var total = 0f

        for (i in 1 until routePoints.size) {
            val prev = routePoints[i - 1]
            val current = routePoints[i]
            // Добавляем расстояние, только если обе точки не на паузе.
            if (!prev.isPause && !current.isPause) {
                total += prev.distanceTo(current)
            }
        }

        return total
    }

    private fun calculateMaxGradient(points: List<RoutePointEntity>): Triple<Double, Double, Float> {
        // Применяем фильтрацию только с GPS-данными
        val filteredPoints = filterAltitudesGpsOnly(
            points
        )

        var altitudeGain = 0.0
        var altitudeLoss = 0.0
        var maxGradient = 0f

        // Для накопления данных по участку
        var segmentAltitudeDiff = 0.0
        var segmentDistance = 0.0

        // Минимальное расстояние для расчёта градиента (например, 20 метров)
        val minSegmentDistance = 20.0

        var prevPoint: RoutePointEntity? = null

        for (point in filteredPoints) {
            if (prevPoint == null) {
                prevPoint = point
                continue
            }

            // Если идет пауза, сбрасываем накопление сегмента
            if (point.isPause || prevPoint.isPause) {
                segmentAltitudeDiff = 0.0
                segmentDistance = 0.0
                prevPoint = point
                continue
            }

            // Расстояние между точками
            val distanceBetweenPoints = prevPoint.distanceTo(point)
            segmentDistance += distanceBetweenPoints

            // Разница высот между точками
            val altDiff = point.altitude - prevPoint.altitude
            segmentAltitudeDiff += altDiff

            // Накопление набора/спуска с фильтрацией малых изменений (более 1 м)
            if (abs(altDiff) > 1) {
                if (altDiff > 0) {
                    altitudeGain += altDiff
                } else {
                    altitudeLoss += abs(altDiff)
                }
            }

            // Если накопленное расстояние больше минимального порога, рассчитываем градиент сегмента
            if (segmentDistance >= minSegmentDistance) {
                val segmentGradient = (segmentAltitudeDiff / segmentDistance) * 100

                // Фильтруем аномальные значения градиента (от 2% до 50%)
                if (abs(segmentGradient) in 2.0..50.0) {
                    maxGradient = kotlin.math.max(maxGradient, abs(segmentGradient).toFloat())
                }

                // Сбрасываем накопленные значения для следующего сегмента
                segmentAltitudeDiff = 0.0
                segmentDistance = 0.0
            }

            prevPoint = point
        }

        return Triple(altitudeGain, altitudeLoss, maxGradient)
    }


    // ==============================
    // Вспомогательные функции
    // ==============================

    // Комбинированный фильтр: медиана + экспоненциальное сглаживание
    fun combinedFilter(points: List<RoutePointEntity>, windowSize: Int = 7, alpha: Double = 0.15): List<RoutePointEntity> {
        val medianFiltered = medianFilter(points, windowSize)
        return exponentialSmooth(medianFiltered, alpha)
    }

    fun medianFilter(points: List<RoutePointEntity>, windowSize: Int = 5): List<RoutePointEntity> {
        val filtered = mutableListOf<RoutePointEntity>()
        for (i in points.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(points.size - 1, i + windowSize / 2)
            val window = points.subList(start, end + 1).map { it.altitude }
            val medianAltitude = window.sorted()[window.size / 2]
            filtered.add(points[i].copy(altitude = medianAltitude))
        }
        return filtered
    }
    fun exponentialSmooth(points: List<RoutePointEntity>, alpha: Double = 0.2): List<RoutePointEntity> {
        val smoothed = mutableListOf<RoutePointEntity>()
        var lastAltitude = points.firstOrNull()?.altitude ?: 0.0
        for (point in points) {
            val smoothedAltitude = alpha * point.altitude + (1 - alpha) * lastAltitude
            smoothed.add(point.copy(altitude = smoothedAltitude))
            lastAltitude = smoothedAltitude
        }
        return smoothed
    }


    private fun smoothAltitude(points: List<RoutePointEntity>, windowSize: Int = 5): List<RoutePointEntity> {
        val smoothed = mutableListOf<RoutePointEntity>()
        for (i in points.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(points.size - 1, i + windowSize / 2)
            val subList = points.subList(start, end + 1)
            val avgAltitude = subList.sumOf { it.altitude } / subList.size
            smoothed.add(points[i].copy(altitude = avgAltitude))
        }
        return smoothed
    }

    /**
     * Применяет простое скользящее среднее на окно размера [windowSize].
     * Возвращает новый список RoutePointEntity со сглаженными heartRate и speed.
     */
    public fun smoothSpeedHeartRate(
        points: List<RoutePointEntity>,
        windowSize: Int = 5
    ): List<RoutePointEntity> {
        if (points.size < windowSize) return points

        val smoothList = mutableListOf<RoutePointEntity>()
        // "окно" для пульса и скорости
        val hrQueue = ArrayDeque<Int>()
        val spdQueue = ArrayDeque<Float>()

        for (i in points.indices) {
            val hr = points[i].heartRate ?: 0
            val spd = points[i].speed ?: 0f

            hrQueue.addLast(hr)
            spdQueue.addLast(spd)

            if (hrQueue.size > windowSize) hrQueue.removeFirst()
            if (spdQueue.size > windowSize) spdQueue.removeFirst()

            val avgHr = hrQueue.average()
            val avgSpd = spdQueue.map { it.toDouble() }.average()

            smoothList.add(
                points[i].copy(
                    heartRate = avgHr.toInt(),
                    speed = avgSpd.toFloat()
                )
            )
        }
        return smoothList
    }

    /**
     * Возвращает массив накопительных дистанций (cumulative distance).
     * Исключая точки, добавленные на паузе
     * Например, distances[i] — это дистанция от первой точки до i-й включительно.
     */
    public fun cumulativeDistance(points: List<RoutePointEntity>): List<Float> {
        if (points.isEmpty()) return emptyList()
        val result = MutableList(points.size) { 0f }
        var sumDist = 0f
        for (i in 1 until points.size) {
            val d = points[i - 1].distanceTo(points[i])
            if (!points[i - 1].isPause && !points[i].isPause) {
                sumDist += d
                result[i] = sumDist
            }
        }
        return result
    }
}

/**
 * Фильтрует высоты в списке RoutePoint, используя только GPS-данные.
 *
 * 1) Проверяем точность GPS (verticalAccuracyMeters). Если точность плохая,
 *    используем предыдущее "хорошее" значение высоты.
 * 2) Ограничиваем слишком резкие скачки высоты, исходя из скорости (speed)
 *    и коэффициента slopeFactor.
 * 3) Применяем экспоненциальное сглаживание.
 *
 * @param points              Список исходных точек
 * @param maxGpsVerticalError Порог допустимой погрешности GPS по высоте
 * @param slopeFactor         Коэффициент максимально допустимого уклона (м / (м/с)).
 *                            Например, 0.3 означает, что за 1 м горизонтального
 *                            движения допускается 0.3 м вертикального изменения.
 * @param alpha               Коэффициент экспоненциального сглаживания (0..1).
 *                            При alpha=0.2 фильтрация более плавная, при alpha=0.8 – реагирует
 *                            быстрее на новые данные, но сильнее подвержена шуму.
 */
fun filterAltitudesGpsOnly(
    points: List<RoutePointEntity>,
    maxGpsVerticalError: Float = 15f,
    slopeFactor: Double = 0.15,
    alpha: Double = 0.3
): List<RoutePointEntity> {
    if (points.isEmpty()) return emptyList()

    val result = mutableListOf<RoutePointEntity>()

    // Начальное сглаженное значение — берем из первой точки
    var smoothedAltitude = points.first().altitude
    var lastTimestamp = points.first().timestamp

    // Добавляем первую точку, сразу с "заглаженной" высотой
    result.add(points.first().copy(altitude = smoothedAltitude))

    for (i in 1 until points.size) {
        val prevFilteredPoint = result.last()
        val currRaw = points[i]

        val dt = (currRaw.timestamp - lastTimestamp) / 1000.0
        // Если время не двигается или идёт назад, просто копируем последнее значение
        if (dt < 0.001) {
            result.add(currRaw.copy(altitude = smoothedAltitude))
            continue
        }

        // 1. Определяем, использовать ли "сырое" GPS-значение
        val gpsIsAccurate = currRaw.verticalAccuracyMeters == null ||
                currRaw.verticalAccuracyMeters <= maxGpsVerticalError
        val rawAltitude = if (gpsIsAccurate) {
            currRaw.altitude
        } else {
            // Если GPS плохо (очень большая погрешность), берём последнее сглаженное
            smoothedAltitude
        }

        // 2. Ограничиваем резкий скачок по вертикали
        val maxAltitudeChange = slopeFactor * currRaw.speed * dt
        val diff = rawAltitude - smoothedAltitude
        val clampedAltitude = if (abs(diff) > maxAltitudeChange) {
            smoothedAltitude + maxAltitudeChange * kotlin.math.sign(diff)
        } else {
            rawAltitude
        }

        // 3. Применяем экспоненциальное сглаживание
        smoothedAltitude = alpha * clampedAltitude + (1 - alpha) * smoothedAltitude

        // Формируем новую точку
        val filteredPoint = currRaw.copy(altitude = smoothedAltitude)
        result.add(filteredPoint)

        lastTimestamp = currRaw.timestamp
    }

    return result
}

/**
 * Сглаживает параметр скорости (speed) в списке точек маршрута с использованием экспоненциального сглаживания.
 *
 * Формула сглаживания:
 *     smoothedSpeed = alpha * rawSpeed + (1 - alpha) * previousSmoothedSpeed
 *
 * @param points Список исходных точек маршрута
 * @param alpha  Коэффициент сглаживания (0 < alpha <= 1). Значение ближе к 0 даст более сильное сглаживание.
 * @return Новый список RoutePoint с обновлённым значением speed, сглаженным по экспоненциальной схеме.
 */
fun smoothSpeedParameter(points: List<RoutePointEntity>, alpha: Float = 0.3f): List<RoutePointEntity> {
    if (points.isEmpty()) return emptyList()

    val smoothedPoints = mutableListOf<RoutePointEntity>()
    // Инициализируем сглаженное значение скорости первой точки
    var smoothedSpeed = points.first().speed
    smoothedPoints.add(points.first().copy(speed = smoothedSpeed))

    // Проходим по оставшимся точкам и применяем экспоненциальное сглаживание
    for (i in 1 until points.size) {
        val rawSpeed = points[i].speed
        smoothedSpeed = alpha * rawSpeed + (1 - alpha) * smoothedSpeed
        smoothedPoints.add(points[i].copy(speed = smoothedSpeed))
    }
    return smoothedPoints
}

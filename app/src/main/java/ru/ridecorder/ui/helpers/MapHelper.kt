package ru.ridecorder.ui.helpers

import com.yandex.mapkit.geometry.Point
import kotlin.math.*

object MapHelper {
    data class CameraData(val center: Point, val zoom: Float)

    fun calculateCameraData(points: List<Point>, mapWidth: Int, mapHeight: Int): CameraData {
        if (points.isEmpty()) return CameraData(Point(0.0, 0.0), 0f)

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        // Находим крайние координаты маршрута
        points.forEach { point ->
            minLat = min(minLat, point.latitude)
            maxLat = max(maxLat, point.latitude)
            minLng = min(minLng, point.longitude)
            maxLng = max(maxLng, point.longitude)
        }

        // Вычисляем центр маршрута как среднее значение крайних координат
        val center = Point((minLat + maxLat) / 2, (minLng + maxLng) / 2)

        // Вспомогательная функция для преобразования широты в радианы с учетом проекции Меркатора
        fun latRad(lat: Double): Double {
            val sin = sin(lat * PI / 180)
            val radX2 = ln((1 + sin) / (1 - sin)) / 2
            return radX2
        }

        // Вычисляем «доли» (fraction) для широты и долготы
        val latFraction = (latRad(maxLat) - latRad(minLat)) / PI * 2
        var lngDiff = maxLng - minLng
        if (lngDiff < 0) lngDiff += 360
        val lngFraction = lngDiff / 360 * 2

        // Стандартный размер тайла для Yandex Maps – 256 пикселей
        // Вычисляем уровень масштабирования для высоты и ширины карты
        val latZoom = if (latFraction == 0.0) Double.MAX_VALUE else ln(mapHeight / 256.0 / latFraction) / ln(2.0)
        val lngZoom = if (lngFraction == 0.0) Double.MAX_VALUE else ln(mapWidth / 256.0 / lngFraction) / ln(2.0)

        // Выбираем минимальный zoom, чтобы гарантировать отображение всего маршрута
        val zoom = min(latZoom, lngZoom).toFloat().coerceIn(0f, 21f)

        return CameraData(center, zoom * 0.94f)
    }
}
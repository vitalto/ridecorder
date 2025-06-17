package ru.ridecorder.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.data.local.database.WorkoutEntity
import java.io.File
import java.io.StringReader
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Сервис для импорта/экспорта GPX
class GpxService @Inject constructor(@ApplicationContext private val context: Context) {

    // Функция для экспорта тренировки и её трека в GPX-формат
    fun exportWorkoutToGpx(workout: WorkoutEntity, trackPoints: List<RoutePointEntity>): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<gpx version=\"1.1\" creator=\"Ridecorder\">\n")
        builder.append("  <trk>\n")
        builder.append("    <name>${workout.name ?: "Workout"}</name>\n")
        builder.append("    <trkseg>\n")
        trackPoints.forEach { point ->
            builder.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
            point.altitude?.let { ele ->
                builder.append("        <ele>$ele</ele>\n")
            }
            point.timestamp?.let { time ->
                builder.append("        <time>${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(time))}</time>\n")
            }
            builder.append("      </trkpt>\n")
        }
        builder.append("    </trkseg>\n")
        builder.append("  </trk>\n")
        builder.append("</gpx>")
        return builder.toString()
    }

    // Функция для импорта GPX-файла и извлечения информации о треке
    // Возвращает пару: базовый объект WorkoutEntity и список TrackPoint
    fun importWorkoutFromGpx(gpxContent: String): Pair<WorkoutEntity, List<RoutePointEntity>>? {
        // Можно задать дефолтное имя для импортированной тренировки
        val workout = WorkoutEntity(name = "Импортированная тренировка", isFinished = true, isDeleted = false, createdAt = Instant.now(), likesCount = 0)
        val trackPoints = mutableListOf<RoutePointEntity>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(gpxContent))
            var eventType = parser.eventType

            var latitude: Double? = null
            var longitude: Double? = null
            var elevation: Double? = null
            var time: Instant? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "trkpt" -> {
                                latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            }
                            "ele" -> {
                                elevation = parser.nextText().toDoubleOrNull()
                            }
                            "time" -> {
                                time = try {
                                    Instant.parse(parser.nextText())
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "trkpt") {
                            if (latitude != null && longitude != null) {
                                trackPoints.add(RoutePointEntity(
                                    workoutId = 0,
                                    latitude = latitude,
                                    longitude = longitude,
                                    altitude = elevation ?: 0.0,
                                    timestamp = time?.toEpochMilli() ?: 0,
                                    isPause = false,
                                    speed = 0f,
                                    bearing = 0f,
                                    accuracy = 1f,
                                    provider = "gpx",
                                    verticalAccuracyMeters = null,
                                    bearingAccuracyDegrees = null,
                                    speedAccuracyMetersPerSecond = null,
                                    barometerAltitude = null,
                                    heartRate = null))
                            }
                            // Сброс значений для следующей точки
                            latitude = null
                            longitude = null
                            elevation = null
                            time = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            return null;
        }
        val pointsWithSpeed = recalcSpeed(trackPoints)
        return Pair(workout, pointsWithSpeed)
    }

    private fun recalcSpeed(points: List<RoutePointEntity>): List<RoutePointEntity> {
        if (points.size < 2) return points
        val updatedPoints = points.toMutableList()
        for (i in 1 until updatedPoints.size) {
            val prev = updatedPoints[i - 1]
            val current = updatedPoints[i]
            val deltaTimeSec = (current.timestamp - prev.timestamp) / 1000.0
            if (deltaTimeSec > 0) {
                // Расстояние между точками (предполагается, что distanceTo реализована корректно)
                val distance = prev.distanceTo(current)
                // Вычисляем скорость (м/с)
                val speed = (distance / deltaTimeSec).toFloat()
                updatedPoints[i] = current.copy(speed = speed)
            }
        }
        return updatedPoints
    }

    fun saveGpxToDownloads(name: String, gpxContent: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ (Android 10+): Используем MediaStore
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.gpx")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/gpx+xml")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(gpxContent.toByteArray())
                    }
                }
            } else {
                // API 28 и ниже: Сохраняем напрямую в Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "$name.gpx")
                file.writeText(gpxContent)
            }
            Toast.makeText(context, "GPX успешно сохранён!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    fun importWorkoutFromFile(uri: Uri): Pair<WorkoutEntity, List<RoutePointEntity>>? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val gpxContent = reader.readText()
                importWorkoutFromGpx(gpxContent)
            } ?: run {
                Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_LONG).show()
                null
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при импорте файла: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }
}

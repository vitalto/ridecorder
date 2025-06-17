package ru.ridecorder.ui.helpers

import ru.ridecorder.R
import ru.ridecorder.di.IResourceProvider
import java.time.Instant
import java.util.Locale

object ConvertHelper {

    /**
     * Форматирование дистанции в километрах (если данные в метрах).
     */
    fun formatDistance(resourceProvider: IResourceProvider, distanceMeters: Float?): String {
        val distanceKm = (distanceMeters ?: 0f) / 1000f
        return String.format(Locale.US, "%.2f %s", distanceKm, resourceProvider.getString(R.string.unit_kilometers))
    }

    /**
     * Форматирование времени (миллисекунды -> ч:м:с).
     */
    fun formatDuration(resourceProvider: IResourceProvider, durationMs: Long?): String {
        val durationSec = (durationMs ?: 0) / 1000
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        return if (hours > 0) {
            resourceProvider.getString(R.string.format_duration_hms, hours, minutes, seconds)
        } else {
            resourceProvider.getString(R.string.format_duration_ms, minutes, seconds)
        }
    }

    /**
     * Форматирование скорости в километрах (если данные в метрах).
     */
    fun formatSpeed(resourceProvider: IResourceProvider, speedMs: Float?): String {
        return String.format(Locale.US, "%.1f %s", (speedMs ?: 0f) * 3.6, resourceProvider.getString(R.string.unit_speed_kph))
    }

    fun formatTimestamp(resourceProvider: IResourceProvider, timestampMillis: Long?, onlyDate: Boolean = false): String {
        if (timestampMillis == null || timestampMillis <= 0)
            return resourceProvider.getString(R.string.placeholder_na)

        val date = java.util.Date(timestampMillis)
        val format = if (onlyDate) "dd.MM.yyyy" else "dd.MM.yyyy HH:mm"
        val dateFormat = java.text.SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun formatTimestamp(resourceProvider: IResourceProvider, timestamp: Instant?, onlyDate: Boolean = false): String {
        if (timestamp == null) return resourceProvider.getString(R.string.placeholder_na)
        return formatTimestamp(resourceProvider, timestamp.toEpochMilli(), onlyDate)
    }
}
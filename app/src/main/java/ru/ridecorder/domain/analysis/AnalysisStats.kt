package ru.ridecorder.domain.analysis

import ru.ridecorder.R
import ru.ridecorder.di.IResourceProvider
import ru.ridecorder.ui.helpers.ConvertHelper

data class AnalysisStats(
    val averageSpeed: Double,
    val maxSpeed: Float,
    val totalDistance: Float,
    val totalAltitudeGain: Double,
    val totalHours: Double,
    val maxPace: Float,
    val averageSpeedGraph: List<GraphDataPoint>,
    val distanceGraph: List<GraphDataPoint>,
    val altitudeGainGraph: List<GraphDataPoint>,
    val paceGraph: List<GraphDataPoint>,
    val timeInMotionGraph: List<GraphDataPoint>,
    val averageHeartRateGraph: List<GraphDataPoint>,
    val averageHeartRate: Int?,
    val caloriesGraph: List<GraphDataPoint>,
    val caloriesBurned: Double
) {
    fun toList(resourceProvider: IResourceProvider): List<Pair<String, String>> {
        return listOf(
            resourceProvider.getString(R.string.calories) to
                    "${"%.1f".format(caloriesBurned)} ${resourceProvider.getString(R.string.unit_calories)}",

            resourceProvider.getString(R.string.stat_avg_speed) to
                    "${"%.1f".format(averageSpeed)} ${resourceProvider.getString(R.string.unit_speed_kph)}",

            resourceProvider.getString(R.string.stat_max_speed) to
                    "${"%.1f".format(maxSpeed)} ${resourceProvider.getString(R.string.unit_speed_kph)}",

            resourceProvider.getString(R.string.distance) to
                    ConvertHelper.formatDistance(resourceProvider, totalDistance),

            resourceProvider.getString(R.string.altitude_gain) to
                    "${"%.0f".format(totalAltitudeGain)} ${resourceProvider.getString(R.string.unit_altitude_meters)}",

            resourceProvider.getString(R.string.stat_time) to
                    "${"%.1f".format(totalHours)} ${resourceProvider.getString(R.string.unit_time_hours)}",

            resourceProvider.getString(R.string.stat_max_pace) to
                    "${"%.1f".format(maxPace)} ${resourceProvider.getString(R.string.unit_pace)}",

            resourceProvider.getString(R.string.avg_heart_rate) to
                    "${if (averageHeartRate != null && averageHeartRate > 0) averageHeartRate else "-"} ${resourceProvider.getString(R.string.unit_heart_rate)}"
        )
    }

}
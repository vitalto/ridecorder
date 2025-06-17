package ru.ridecorder.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import ru.ridecorder.domain.analysis.RouteAnalytics
import ru.ridecorder.ui.common.CompareChartCard
import ru.ridecorder.ui.common.ErrorScreen
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.helpers.ConvertHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareWorkoutsScreen(
    viewModel: CompareWorkoutsViewModel = hiltViewModel(),
    firstWorkoutId: Long,
    secondWorkoutId: Long,
    firstIsUserWorkout: Boolean,
    secondIsUserWorkout: Boolean,
    onBack: () -> Unit
) {
    val errorMessage by viewModel.errorMessage.collectAsState()

    val firstWorkout by viewModel.firstWorkout.collectAsState()
    val secondWorkout by viewModel.secondWorkout.collectAsState()

    val firstAnalytics by viewModel.firstWorkoutAnalytics.collectAsState()
    val secondAnalytics by viewModel.secondWorkoutAnalytics.collectAsState()

    LaunchedEffect(firstWorkoutId, secondWorkoutId) {
        viewModel.loadWorkouts(
            firstWorkoutId = firstWorkoutId,
            secondWorkoutId = secondWorkoutId,
            firstIsUserWorkout = firstIsUserWorkout,
            secondIsUserWorkout = secondIsUserWorkout
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.compare_workouts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            if (errorMessage != null) {
                ErrorScreen(
                    message = errorMessage
                ) {
                    // Повторная загрузка, если была ошибка
                    viewModel.loadWorkouts(
                        firstWorkoutId,
                        secondWorkoutId,
                        firstIsUserWorkout,
                        secondIsUserWorkout
                    )
                }
            } else if (firstWorkout == null || secondWorkout == null || firstAnalytics == null || secondAnalytics == null) {
                LoadingScreen(Modifier.padding(paddingValues))
            } else {
                val firstPoints = firstWorkout!!.points
                val secondPoints = secondWorkout!!.points

                // Подготовим списки для графиков
                val firstSpeedTime = RouteStatsCalculator.speedOverTime(firstPoints)
                val secondSpeedTime = RouteStatsCalculator.speedOverTime(secondPoints)
                val firstAltTime = RouteStatsCalculator.altitudeOverTime(firstPoints)
                val secondAltTime = RouteStatsCalculator.altitudeOverTime(secondPoints)

                // Аналогично по дистанции
                val firstSpeedDistance = RouteStatsCalculator.speedOverDistance(firstPoints)
                val secondSpeedDistance = RouteStatsCalculator.speedOverDistance(secondPoints)
                val firstAltDistance = RouteStatsCalculator.altitudeOverDistance(firstPoints)
                val secondAltDistance = RouteStatsCalculator.altitudeOverDistance(secondPoints)

                // По пульсу
                val firstHeartRateDistance = RouteStatsCalculator.heartRateOverDistance(firstPoints)
                val secondHeartRateDistance = RouteStatsCalculator.heartRateOverDistance(secondPoints)
                val firstHeartRateTime = RouteStatsCalculator.heartRateOverTime(firstPoints)
                val secondHeartRateTime = RouteStatsCalculator.heartRateOverTime(secondPoints)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Заголовок с названиями/датами
                    CompareWorkoutsHeader(
                        firstWorkout = firstWorkout!!.workout,
                        secondWorkout = secondWorkout!!.workout
                    )

                    // Основная таблица сравнения
                    CompareStatsTable(
                        firstWorkout = firstWorkout!!.workout,
                        secondWorkout = secondWorkout!!.workout,
                        firstAnalytics = firstAnalytics!!,
                        secondAnalytics = secondAnalytics!!
                    )

                    // Раздел с графиками
                    CompareChartCard(
                        title = stringResource(id = R.string.chart_speed_time),
                        firstData = firstSpeedTime,
                        secondData = secondSpeedTime,
                        yUnit = stringResource(id = R.string.unit_speed_kph),
                        xTitle = stringResource(id = R.string.x_axis_time_seconds)
                    )

                    CompareChartCard(
                        title = stringResource(id = R.string.chart_speed_distance),
                        firstData = firstSpeedDistance,
                        secondData = secondSpeedDistance,
                        yUnit = stringResource(id = R.string.unit_speed_kph),
                        xTitle = stringResource(id = R.string.x_axis_distance_meters)
                    )

                    CompareChartCard(
                        title = stringResource(id = R.string.chart_altitude_time),
                        firstData = firstAltTime,
                        secondData = secondAltTime,
                        yUnit = stringResource(id = R.string.unit_altitude_meters),
                        xTitle = stringResource(id = R.string.x_axis_time_seconds)
                    )

                    CompareChartCard(
                        title = stringResource(id = R.string.chart_altitude_distance),
                        firstData = firstAltDistance,
                        secondData = secondAltDistance,
                        yUnit = stringResource(id = R.string.unit_altitude_meters),
                        xTitle = stringResource(id = R.string.x_axis_distance_meters)
                    )

                    CompareChartCard(
                        title = stringResource(id = R.string.chart_heart_rate_distance),
                        firstData = firstHeartRateDistance,
                        secondData = secondHeartRateDistance,
                        yUnit = stringResource(id = R.string.unit_heart_rate),
                        xTitle = stringResource(id = R.string.x_axis_distance_meters)
                    )

                    CompareChartCard(
                        title = stringResource(id = R.string.chart_heart_rate_time),
                        firstData = firstHeartRateTime,
                        secondData = secondHeartRateTime,
                        yUnit = "BPM",
                        xTitle = stringResource(id = R.string.x_axis_time_seconds)
                    )
                }
            }
        }
    )
}

/**
 * Шапка экрана – короткие сведения: название или дата.
 */
@Composable
private fun CompareWorkoutsHeader(
    firstWorkout: WorkoutEntity,
    secondWorkout: WorkoutEntity
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = firstWorkout.name.takeIf { !it.isNullOrBlank() }
                    ?: stringResource(id = R.string.workout_with_id, firstWorkout.id),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = ConvertHelper.formatTimestamp(ContextResourceProvider(context), firstWorkout.startTimestamp, true),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = secondWorkout.name.takeIf { !it.isNullOrBlank() }
                    ?: stringResource(id = R.string.workout_with_id, secondWorkout.id),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = ConvertHelper.formatTimestamp(ContextResourceProvider(context), secondWorkout.startTimestamp, true),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Таблица/список, где сравниваем ключевые характеристики:
 * - дистанция
 * - длительность
 * - средняя скорость
 * - суммарный набор высоты и т.д.
 * Плюс краткая разница (в процентах).
 */
@Composable
fun CompareStatsTable(
    firstWorkout: WorkoutEntity,
    secondWorkout: WorkoutEntity,
    firstAnalytics: RouteAnalytics,
    secondAnalytics: RouteAnalytics
) {
    val context = LocalContext.current

    // Пример, как можно высчитать разницу в процентах
    fun percentDiff(value1: Float?, value2: Float?): String {
        if (value1 == null || value2 == null || value1 == 0f) return "–"
        val diff = ((value2 - value1) / value1) * 100.0
        return String.format(Locale.US, "%.1f%%", diff)
    }
    fun percentDiff(value1: Double?, value2: Double?): String {
        if (value1 == null || value2 == null || value1 == 0.0) return "–"
        val diff = ((value2 - value1) / value1) * 100.0
        return String.format(Locale.US, "%.1f%%", diff)
    }

    // Примеры метрик
    val dist1 = firstWorkout.distance
    val dist2 = secondWorkout.distance
    val distDiff = percentDiff(dist1, dist2)

    // Преобразуем в км/ч
    val avgSpeed1 = firstAnalytics.averageSpeedMps * 3.6
    val avgSpeed2 = secondAnalytics.averageSpeedMps * 3.6
    val speedDiff = percentDiff(avgSpeed1, avgSpeed2)

    val dur1 = firstWorkout.duration
    val dur2 = secondWorkout.duration
    val durDiff = percentDiff(dur1?.toDouble(), dur2?.toDouble())

    val gain1 = firstAnalytics.totalAltitudeGain
    val gain2 = secondAnalytics.totalAltitudeGain
    val gainDiff = percentDiff(gain1, gain2)

    val hr1 = firstAnalytics.avgHeartRage
    val hr2 = secondAnalytics.avgHeartRage
    val hrDiff = percentDiff(hr1?.toDouble(), hr2?.toDouble())

    val cal1 = firstAnalytics.caloriesBurned
    val cal2 = secondAnalytics.caloriesBurned
    val calDiff = percentDiff(hr1?.toDouble(), hr2?.toDouble())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.comparative_metrics),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Строки в таблице
            CompareRow(
                metricName = stringResource(id = R.string.distance),
                firstValue = ConvertHelper.formatDistance(ContextResourceProvider(context), dist1),
                secondValue = ConvertHelper.formatDistance(ContextResourceProvider(context), dist2),
                diff = distDiff
            )
            CompareRow(
                metricName = stringResource(id = R.string.duration),
                firstValue = ConvertHelper.formatDuration(ContextResourceProvider(context), dur1),
                secondValue = ConvertHelper.formatDuration(ContextResourceProvider(context), dur2),
                diff = durDiff
            )
            CompareRow(
                metricName = stringResource(id = R.string.avg_speed),
                firstValue = String.format(Locale.US, "%.1f %s", avgSpeed1, stringResource(id = R.string.unit_speed_kph)),
                secondValue = String.format(Locale.US, "%.1f %s", avgSpeed2, stringResource(id = R.string.unit_speed_kph)),
                diff = speedDiff
            )
            CompareRow(
                metricName = stringResource(id = R.string.altitude_gain),
                firstValue = String.format(Locale.US, "%.0f ${stringResource(id = R.string.unit_distance_meters)}", gain1),
                secondValue = String.format(Locale.US, "%.0f ${stringResource(id = R.string.unit_distance_meters)}", gain2),
                diff = gainDiff
            )
            CompareRow(
                metricName = stringResource(id = R.string.avg_heart_rate),
                firstValue = "$hr1 ${stringResource(id = R.string.unit_heart_rate)}",
                secondValue = "$hr2 ${stringResource(id = R.string.unit_heart_rate)}",
                diff = hrDiff
            )
            CompareRow(
                metricName = stringResource(id = R.string.calories),
                firstValue = String.format(Locale.US, "%.0f ${stringResource(id = R.string.unit_calories)}", cal1),
                secondValue = String.format(Locale.US, "%.0f ${stringResource(id = R.string.unit_calories)}", cal2),
                diff = calDiff
            )
        }
    }
}

@Composable
fun CompareRow(
    metricName: String,
    firstValue: String,
    secondValue: String,
    diff: String
) {
    // Можно упростить/усложнить отображение на своё усмотрение
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = metricName,
            modifier = Modifier.weight(0.4f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = firstValue,
            modifier = Modifier.weight(0.3f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = secondValue,
            modifier = Modifier.weight(0.3f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Можно показать diff отдельно или рядом
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = diff,
            modifier = Modifier.weight(0.3f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

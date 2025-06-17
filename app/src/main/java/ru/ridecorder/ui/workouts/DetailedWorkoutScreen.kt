package ru.ridecorder.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.domain.analysis.GraphDataPoint
import ru.ridecorder.domain.analysis.RecoveryPhaseAnalyzer
import ru.ridecorder.domain.analysis.RouteAnalytics
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import ru.ridecorder.domain.analysis.WorkloadEfficiency
import ru.ridecorder.domain.model.RideType
import ru.ridecorder.domain.model.Visibility
import ru.ridecorder.ui.common.ChartCard
import ru.ridecorder.ui.common.ErrorScreen
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.helpers.ConvertHelper
import ru.ridecorder.ui.common.MapScreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedWorkoutScreen(
    viewModel: DetailedWorkoutViewModel = hiltViewModel(),
    workoutId: Long,
    onBack: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    isUserWorkout: Boolean,
    onCompare: () -> Unit
) {
    val workoutWithPoints by viewModel.workoutWithPoints.collectAsState()
    val cameraData by viewModel.cameraData.collectAsState()
    val routeAnalytics by viewModel.routeAnalytics.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp * LocalDensity.current.density
    val screenHeight = configuration.screenHeightDp * LocalDensity.current.density

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId, screenWidth, screenHeight, isUserWorkout)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.workout_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    if(isUserWorkout) {
                        IconButton(onClick = { onEditWorkout(workoutId) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(id = R.string.edit_workout)
                            )
                        }
                        IconButton(onClick = { viewModel.exportWorkout(workoutId) }) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = stringResource(id = R.string.export_workout)
                            )
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            if(errorMessage != null){
                ErrorScreen(errorMessage)
                {
                    viewModel.loadWorkout(workoutId, screenWidth, screenHeight, isUserWorkout)
                }
            }
            else if (workoutWithPoints == null || routeAnalytics == null) {
                LoadingScreen(Modifier.padding(paddingValues))
            }
            else {
                val workout = workoutWithPoints!!.workout
                val routePoints = workoutWithPoints!!.points.filter { !it.isPause }

                val speedTimeData = RouteStatsCalculator.speedOverTime(routePoints)
                val speedDistanceData = RouteStatsCalculator.speedOverDistance(routePoints)
                val altitudeTimeData = RouteStatsCalculator.altitudeOverTime(routePoints)
                val altitudeDistanceData = RouteStatsCalculator.altitudeOverDistance(routePoints)
                val heartRateTimeData = RouteStatsCalculator.heartRateOverTime(routePoints)
                val heartRateDistanceData = RouteStatsCalculator.heartRateOverDistance(routePoints)

                val workloadEfficiencyTimeData = WorkloadEfficiency.workloadEfficiencyOverTime(routePoints)
                val workloadEfficiencyDistanceData = WorkloadEfficiency.workloadEfficiencyOverDistance(routePoints)
                val cumulativeLoadTimeData = RouteStatsCalculator.cumulativeLoadOverTime(routePoints)
                val recoveryPhasesData = RecoveryPhaseAnalyzer.recoveryPhases(routePoints)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Верхний блок (Hero-Header) с названием тренировки и ключевыми данными
                    HeroHeader(workout = workout)

                    // Карта
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Карта с маршрутом
                        MapScreen(
                            points = routePoints,
                            badAccuracy = false,
                            showCurrentLocation = false,
                            defaultZoom = cameraData?.zoom ?: 18f,
                            defaultPoint = cameraData?.center,
                            darkMode = true
                        )
                    }

                    // Описание тренировки
                    workout.description?.takeIf { it.isNotBlank() }?.let { description ->
                        WorkoutDetailSection(title = stringResource(id = R.string.workout_description), value = description)
                    }

                    // Карточка подробной информации
                    ExtraStatsSection(routeAnalytics!!)

                    ChartSection(
                        speedTimeData = speedTimeData,
                        speedDistanceData = speedDistanceData,
                        altitudeTimeData = altitudeTimeData,
                        altitudeDistanceData = altitudeDistanceData,
                        heartRateTimeData,
                        heartRateDistanceData,

                        workloadEfficiencyTimeData = workloadEfficiencyTimeData,
                        workloadEfficiencyDistanceData = workloadEfficiencyDistanceData,
                        cumulativeLoadTimeData = cumulativeLoadTimeData,
                        recoveryPhasesData = recoveryPhasesData
                    )

                    WorkoutMetaSection(workout)

                    // Кнопка "Сравнить с другой тренировкой"
                    Button(
                        onClick = {
                            onCompare()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = stringResource(id = R.string.compare_workout_button))
                    }
                }
            }
        }
    )
}

@Composable
private fun HeroHeader(workout: WorkoutEntity) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (workout.name.isNullOrBlank()) {
                stringResource(
                    id = R.string.workout_from_date,
                    ConvertHelper.formatTimestamp(ContextResourceProvider(context), workout.startTimestamp, true)
                )
            } else {
                workout.name!!
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Блок с ключевыми цифрами: дистанция, длительность, дата
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyInfoBlock(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = stringResource(id = R.string.distance),
                value = ConvertHelper.formatDistance(ContextResourceProvider(context), workout.distance)
            )
            KeyInfoBlock(
                icon = Icons.Filled.Timer,
                title = stringResource(id = R.string.duration),
                value = ConvertHelper.formatDuration(ContextResourceProvider(context), workout.duration)
            )
            KeyInfoBlock(
                icon = Icons.Filled.CalendarMonth,
                title = stringResource(id = R.string.date),
                value = ConvertHelper.formatTimestamp(ContextResourceProvider(context), workout.endTimestamp!!.toEpochMilli(), true)
            )
        }
    }
}

@Composable
private fun KeyInfoBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Дополнительная статистика: высоты, темп, набор/спуск и пр.
 */
@Composable
fun ExtraStatsSection(
    analytics: RouteAnalytics
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Получаем единицы измерения из ресурсов
            val unitCalories = stringResource(id = R.string.unit_calories)
            val unitSpeed = stringResource(id = R.string.unit_speed_kph)
            val unitAltitude = stringResource(id = R.string.unit_altitude_meters)
            val unitPace = stringResource(id = R.string.unit_pace)
            val unitHeartRate = stringResource(id = R.string.unit_heart_rate)

            // Форматируем и выводим показатели, используя параметризованные строки
            Text(
                text = stringResource(
                    id = R.string.calories_label_format,
                    String.format(Locale.US, "%.2f", analytics.caloriesBurned) + " " + unitCalories
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.avg_speed_label_format,
                    String.format(Locale.US, "%.2f", analytics.averageSpeedMps * 3.6) + " " + unitSpeed
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.max_speed_label_format,
                    String.format(Locale.US, "%.2f", analytics.maxSpeedMps * 3.6) + " " + unitSpeed
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.min_speed_label_format,
                    String.format(Locale.US, "%.2f", analytics.minSpeedMps * 3.6) + " " + unitSpeed
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.altitude_range_label_format,
                    String.format(Locale.US, "%.0f", analytics.altitudeRange) + " " + unitAltitude
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.altitude_gain_label_format,
                    String.format(Locale.US, "%.0f", analytics.totalAltitudeGain) + " " + unitAltitude
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.altitude_loss_label_format,
                    String.format(Locale.US, "%.0f", analytics.totalAltitudeLoss) + " " + unitAltitude
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.avg_pace_label_format,
                    String.format(Locale.US, "%.1f", analytics.averagePaceSecPerKm / 60) + " " + unitPace
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.max_gradient_label_format,
                    String.format(Locale.US, "%.2f", analytics.maxGradientPercent) + " %"
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (analytics.avgHeartRage != null && analytics.avgHeartRage > 0) {
                Text(
                    text = stringResource(
                        id = R.string.avg_heart_rate_label_format,
                        analytics.avgHeartRage.toString() + " " + unitHeartRate
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        id = R.string.max_heart_rate_label_format,
                        analytics.maxHeartRate.toString() + " " + unitHeartRate
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        id = R.string.min_heart_rate_label_format,
                        analytics.minHeartRate.toString() + " " + unitHeartRate
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChartSection(
    speedTimeData: List<GraphDataPoint>,
    speedDistanceData: List<GraphDataPoint>,
    altitudeTimeData: List<GraphDataPoint>,
    altitudeDistanceData: List<GraphDataPoint>,
    heartRateTimeData: List<GraphDataPoint>?,
    heartRateDistanceData: List<GraphDataPoint>?,
    workloadEfficiencyTimeData: List<GraphDataPoint>,
    workloadEfficiencyDistanceData: List<GraphDataPoint>,
    cumulativeLoadTimeData: List<GraphDataPoint>,
    recoveryPhasesData: List<GraphDataPoint>
) {
    ChartCard(stringResource(id = R.string.chart_speed_time), speedTimeData, stringResource(id = R.string.unit_speed_kph), stringResource(id = R.string.x_axis_time_seconds))
    ChartCard(stringResource(id = R.string.chart_speed_distance), speedDistanceData, stringResource(id = R.string.unit_speed_kph), stringResource(id = R.string.x_axis_distance_meters))
    ChartCard(stringResource(id = R.string.chart_altitude_time), altitudeTimeData, stringResource(id = R.string.unit_altitude_meters), stringResource(id = R.string.x_axis_time_seconds))
    ChartCard(stringResource(id = R.string.chart_altitude_distance), altitudeDistanceData, stringResource(id = R.string.unit_altitude_meters), stringResource(id = R.string.x_axis_distance_meters))
    if(heartRateTimeData != null)
        ChartCard(stringResource(id = R.string.chart_heart_rate_time), heartRateTimeData, stringResource(id = R.string.unit_heart_rate), stringResource(id = R.string.x_axis_time_seconds))
    if(heartRateDistanceData != null)
        ChartCard(stringResource(id = R.string.chart_heart_rate_distance), heartRateDistanceData, stringResource(id = R.string.unit_heart_rate), stringResource(id = R.string.x_axis_distance_meters))

    if(workloadEfficiencyTimeData.isNotEmpty())
        ChartCard(stringResource(id = R.string.chart_workload_efficiency_time), workloadEfficiencyTimeData, stringResource(id = R.string.unit_workload_efficiency), stringResource(id = R.string.x_axis_time_minutes))
    if(workloadEfficiencyDistanceData.isNotEmpty())
        ChartCard(stringResource(id = R.string.chart_workload_efficiency_distance), workloadEfficiencyDistanceData, stringResource(id = R.string.unit_workload_efficiency), stringResource(id = R.string.x_axis_distance_kilometers))
    if(cumulativeLoadTimeData.isNotEmpty() && cumulativeLoadTimeData.any { it.y != 0f })
        ChartCard(stringResource(id = R.string.chart_cumulative_load), cumulativeLoadTimeData, stringResource(R.string.unit_cu), stringResource(id = R.string.x_axis_time_minutes))
    if(recoveryPhasesData.isNotEmpty())
        ChartCard(stringResource(id = R.string.chart_recovery_phases), recoveryPhasesData, stringResource(id = R.string.unit_recovery_rate), stringResource(id = R.string.x_axis_time_minutes))
}

/**
 * Компонент отображения секции с дополнительной информацией (описание, тип тренировки и т. д.)
 */
@Composable
private fun WorkoutDetailSection(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun WorkoutMetaSection(workout: WorkoutEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Тип тренировки
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = stringResource(id = R.string.workout_type),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = workout.type?.let { RideType.fromString(it) }?.displayName
                        ?: stringResource(id = R.string.default_workout_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Видимость
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = stringResource(id = R.string.visibility),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = workout.whoCanView?.let { Visibility.fromString(it) }?.displayName
                        ?: stringResource(id = R.string.default_visibility),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package ru.ridecorder.ui.analysis

import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.ridecorder.R
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.local.preferences.UserDataStore
import ru.ridecorder.domain.analysis.AnalysisStats
import ru.ridecorder.domain.analysis.GraphDataPoint
import ru.ridecorder.domain.analysis.RouteAnalytics
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import ru.ridecorder.data.repository.WorkoutRepository
import java.util.Calendar
import javax.inject.Inject

enum class Period(@StringRes val displayNameRes: Int) {
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    CUSTOM(R.string.period_custom)
}

data class AnalysisUiState(
    val selectedPeriod: Period = Period.WEEK,
    val stats: AnalysisStats? = null,
    val isLoading: Boolean = false,
    val customDataRange: Pair<Long?, Long?>? = null,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userDataStore: UserDataStore
) : ViewModel() {
    private val _uiState = mutableStateOf(AnalysisUiState(isLoading = true))
    val uiState: State<AnalysisUiState> = _uiState

    init {
        loadStats()
    }

    fun selectPeriod(period: Period) {
        if (_uiState.value.selectedPeriod != period) {
            _uiState.value = _uiState.value.copy(selectedPeriod = period, customDataRange = null)
            loadStats()
        }
    }
    fun setCustomPeriod(customDataRange: Pair<Long?, Long?>?) {
        _uiState.value = _uiState.value.copy(customDataRange = customDataRange)
        loadStats()
    }

    private fun loadStats() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val workouts = getWorkoutsForPeriod() ?: return@launch

            val stats = calculateStats(workouts)

            _uiState.value = _uiState.value.copy(
                stats = stats,
                isLoading = false
            )
        }
    }

    private suspend fun getWorkoutsForPeriod(): List<WorkoutEntity>? {
        val (startDate, endDate) = when (_uiState.value.selectedPeriod) {
            Period.WEEK -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis()
                start to end
            }
            Period.MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis()
                start to end
            }
            Period.CUSTOM -> {
                val start = _uiState.value.customDataRange?.first
                val end = _uiState.value.customDataRange?.second
                if (start == null || end == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false) // Избегаем бесконечной загрузки
                    return null
                }
                start to end
            }
        }

        return workoutRepository.getWorkoutsBetweenDates(startDate, endDate)
    }


    private suspend fun calculateStats(workouts: List<WorkoutEntity>): AnalysisStats? {
        if (workouts.isEmpty()) return null
        val routeAnalytics = arrayListOf<RouteAnalytics>()

        for (workout in workouts){
            val points = workoutRepository.getAllRoutePoints(workout.id).firstOrNull() ?: continue
            val analytics = RouteStatsCalculator.analyze(workout, points, userDataStore.userFlow.firstOrNull()?.gender)
            routeAnalytics.add(analytics)
        }

        val avgHeartRateList = routeAnalytics
            .filter { it.avgHeartRage != null && it.avgHeartRage > 0 }
            .map { it.avgHeartRage!! }


        return AnalysisStats(
            averageSpeed = routeAnalytics.map { it.averageSpeedMps * 3.6 }.average(),
            maxSpeed = routeAnalytics.maxOfOrNull { it.maxSpeedMps * 3.6f } ?: 0f,
            totalDistance = routeAnalytics.map { it.totalDistanceMeters }.sum(),
            totalAltitudeGain = routeAnalytics.sumOf { it.totalAltitudeGain },
            totalHours = routeAnalytics.map { it.totalDurationSeconds }.sum() / 3600.0,
            maxPace = routeAnalytics.maxOfOrNull { it.averagePaceSecPerKm / 60 } ?: 0f,

            averageSpeedGraph = getAverageSpeedGraph(routeAnalytics),
            distanceGraph = getDistanceGraph(routeAnalytics),
            altitudeGainGraph = getAltitudeGainGraph(routeAnalytics),
            paceGraph = getPaceGraph(routeAnalytics),
            timeInMotionGraph = getTimeInMotionGraph(routeAnalytics),
            averageHeartRate = if(avgHeartRateList.isEmpty()) 0 else avgHeartRateList.average().toInt(),
            averageHeartRateGraph = getAverageHeartRateGraph(routeAnalytics),
            caloriesGraph = getCaloriesGraph(routeAnalytics),
            caloriesBurned = routeAnalytics.sumOf { it.caloriesBurned }
        )
    }

    private fun getCaloriesGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.caloriesBurned.toFloat())
        }
    }

    private fun getAverageHeartRateGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            // Если avgHeartRage равен null, то задаём 0 (или можно выбрать другое значение/обработку)
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.avgHeartRage?.toFloat() ?: 0f)
        }.filter { it.y > 0 }
    }

    private fun getAverageSpeedGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            // Переводим скорость из м/с в км/ч
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.averageSpeedMps * 3.6f)
        }
    }

    private fun getDistanceGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.totalDistanceMeters)
        }
    }

    private fun getAltitudeGainGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.totalAltitudeGain.toFloat())
        }
    }

    private fun getPaceGraph(workouts: List<RouteAnalytics>): List<GraphDataPoint> {
        return workouts.mapIndexed { index, workout ->
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.averagePaceSecPerKm / 60f)
        }
    }

    private fun getTimeInMotionGraph(routeAnalytics: List<RouteAnalytics>): List<GraphDataPoint> {
        return routeAnalytics.mapIndexed { index, workout ->
            // Переводим длительность тренировки из секунд в часы
            GraphDataPoint(x = (index + 1).toFloat(), y = workout.totalDurationSeconds / 3600f)
        }
    }
}
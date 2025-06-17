package ru.ridecorder.domain.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.ridecorder.data.local.database.RoutePointEntity
import ru.ridecorder.domain.analysis.TrackingStats
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import javax.inject.Inject

class RoutePointsManager @Inject constructor() {
    private val _routePointsFlow = MutableStateFlow<List<RoutePointEntity>>(emptyList())
    val routePointsFlow: StateFlow<List<RoutePointEntity>> = _routePointsFlow.asStateFlow()

    private val _stats = MutableStateFlow(TrackingStats())
    val stats: StateFlow<TrackingStats> = _stats.asStateFlow()

    private val _badAccuracy = MutableStateFlow(false)
    val badAccuracy: StateFlow<Boolean> = _badAccuracy.asStateFlow()

    fun setPoints(points: List<RoutePointEntity>) {
        _routePointsFlow.value = points
    }

    fun addPoint(point: RoutePointEntity): Boolean {
        val currentPoints = _routePointsFlow.value

        if (currentPoints.isNotEmpty()) {
            val lastPoint = currentPoints.last()
            if (lastPoint.distanceTo(point) < 1.0) return false
        }

        _routePointsFlow.value += point
        _stats.value = calculateStats()
        return true
    }

    private fun calculateStats(): TrackingStats {
        return RouteStatsCalculator.calculateTrackingStats(_routePointsFlow.value)
    }

    fun clear() {
        _routePointsFlow.value = emptyList()
    }

    fun setIsBadAccuracy(badAccuracy: Boolean) {
        _badAccuracy.value = badAccuracy
    }
}

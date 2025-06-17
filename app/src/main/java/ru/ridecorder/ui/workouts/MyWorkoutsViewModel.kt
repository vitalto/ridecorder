package ru.ridecorder.ui.workouts

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.GpxService
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.data.repository.WorkoutRepository
import ru.ridecorder.domain.analysis.RouteStatsCalculator
import java.time.Instant
import javax.inject.Inject

 @HiltViewModel
class MyWorkoutsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val gpxService: GpxService
) : ViewModel() {

    // Используем map, чтобы преобразовать Flow<List<Workout>> в Flow<MyWorkoutsUiState>.
    // По умолчанию, пока данные не пришли, считаем что isLoading = true (стартовое значение в stateIn).
    val uiState = workoutRepository.getAllWorkouts()
        .map { workouts ->
            MyWorkoutsUiState(
                isLoading = false,
                workouts = workouts
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Если пользователь, например, вернется на экран спустя 3 секунды, Flow сразу выдаст данные без нового запроса в БД.
            initialValue = MyWorkoutsUiState(isLoading = true)
        )

     private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
     val syncStatus = _syncStatus.asStateFlow()

     init {
         syncData()
     }

    fun syncData() {
        viewModelScope.launch {
             _syncStatus.value = SyncStatus.Syncing // Устанавливаем статус "Синхронизация..."
             val success = workoutRepository.sync()
             if (success) {
                 _syncStatus.value = SyncStatus.Success
             } else {
                 _syncStatus.value = SyncStatus.Error
             }
             delay(1000) // Показываем статус пару секунд
             _syncStatus.value = SyncStatus.Idle
        }
     }

     fun exportWorkouts() {
         viewModelScope.launch {
             val workoutsWithPoints = workoutRepository.getAllWorkoutsWithPoints().firstOrNull() ?: return@launch
             workoutsWithPoints.forEach { workoutWithPoints ->
                 val workoutGpx = gpxService.exportWorkoutToGpx(workoutWithPoints.workout, workoutWithPoints.points)
                 gpxService.saveGpxToDownloads("workout${workoutWithPoints.workout.id}", workoutGpx)
             }
         }
     }

     fun importWorkouts(uri: Uri) {
         viewModelScope.launch {
             val workoutWithPoints = gpxService.importWorkoutFromFile(uri) ?: return@launch
             val workout = workoutWithPoints.first
             var points = workoutWithPoints.second

             val stats = RouteStatsCalculator.calculateTrackingStats(points)
             val sortedPoints = points.sortedBy { point -> point.timestamp }
             workout.startTimestamp = Instant.ofEpochMilli(sortedPoints.first().timestamp)
             workout.endTimestamp = Instant.ofEpochMilli(sortedPoints.last().timestamp)
             workout.averageSpeed = stats.averageSpeed
             workout.duration = stats.activeDuration
             workout.distance = stats.distance

             val workoutId = workoutRepository.insertWorkoutLocally(workout)
             points = points.map { it.copy(workoutId = workoutId)}
             workoutRepository.insertRoutePoints(points)
             syncData()
         }
     }
 }

data class MyWorkoutsUiState(
    val isLoading: Boolean = false,
    val workouts: List<WorkoutEntity> = emptyList()
)

sealed class SyncStatus {
    data object Idle : SyncStatus() // Ничего не отображается
    data object Syncing : SyncStatus() // Отображается "Синхронизация с сервером..."
    data object Success : SyncStatus() // Отображается "Синхронизация успешна" (зеленый фон)
    data object Error : SyncStatus() // Отображается "Ошибка синхронизации" (красный фон)
}
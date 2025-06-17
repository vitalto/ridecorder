package ru.ridecorder.ui.workouts

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ru.ridecorder.NavRoutes
import ru.ridecorder.R
import ru.ridecorder.data.local.database.WorkoutEntity
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.helpers.ConvertHelper


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWorkoutsScreen(
    viewModel: MyWorkoutsViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState = viewModel.uiState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.my_workouts_title)) },
                actions = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ImportWorkoutDropdownMenuItem(viewModel) {
                                expanded = false
                            }
                            DropdownMenuItem(onClick = {
                                viewModel.exportWorkouts()
                                expanded = false  // Закрываем меню
                            }, text = {
                                Text(stringResource(R.string.export_workout))
                            })
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val realPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(realPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize() // Плавное изменение размера
            ) {
                SyncStatusMessage(syncStatus)
            }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    uiState.value.isLoading -> {
                        LoadingScreen()
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = syncStatus == SyncStatus.Syncing,
                            onRefresh = {
                                viewModel.syncData()
                            },
                        ) {
                            if(uiState.value.workouts.isEmpty()){
                                EmptyWorkoutsScreen()
                            }
                            else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.value.workouts) { workout ->
                                        WorkoutItem(workout = workout) {
                                            navController.navigate(NavRoutes.Workout.createRoute(workout.id, null))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Составной экран для пустого списка тренировок.
 * Можно выводить иллюстрацию, текст, кнопку «Создать тренировку» и т.д.
 */
@Composable
fun EmptyWorkoutsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.ic_user_location),
                contentDescription = stringResource(R.string.empty_feed_icon_desc),
                modifier = Modifier
                    .size(120.dp)
                    .alpha(0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_workouts_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_workouts_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

        }
    }
}

/**
 * Один элемент списка тренировок.
 */
@Composable
fun WorkoutItem(workout: WorkoutEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Название тренировки
            Text(
                text = if (workout.name.isNullOrBlank()) {
                    stringResource(
                        R.string.ride_name_default_format,
                        ConvertHelper.formatTimestamp(ContextResourceProvider(context), workout.startTimestamp, true)
                    )
                } else {
                    workout.name!!
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Если есть описание, выводим
            workout.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Дата и время начала
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.start_time,
                    ConvertHelper.formatTimestamp(ContextResourceProvider(context), workout.startTimestamp)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Блок с иконками, расстоянием, временем и лайками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Расстояние
                IconTextRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    text = ConvertHelper.formatDistance(ContextResourceProvider(context), workout.distance)
                )
                // Время (продолжительность)
                IconTextRow(
                    icon = Icons.Default.Schedule,
                    text = ConvertHelper.formatDuration(ContextResourceProvider(context), workout.duration)
                )
                // Лайки
                IconTextRow(
                    icon = Icons.Default.Favorite,
                    text = if(workout.likesCount > 0) workout.likesCount.toString() else ""
                )
            }
        }
    }
}



/**
 * Небольшая вспомогательная функция, выводящая иконку + текст в ряд.
 */
@Composable
fun IconTextRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SyncStatusMessage(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
    var lastSyncStatus by remember { mutableStateOf<SyncStatus>(SyncStatus.Syncing) }
    if (syncStatus != SyncStatus.Idle) {
        lastSyncStatus = syncStatus
    }

    val isVisible = syncStatus != SyncStatus.Idle

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val backgroundColor = when (lastSyncStatus) {
            SyncStatus.Syncing -> Color(0xFFB0BEC5) // Мягкий серый
            SyncStatus.Success -> Color(0xFF66BB6A) // Приятный зеленый
            SyncStatus.Error -> Color(0xFFEF5350)   // Светло-красный
            else -> Color.Transparent
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (lastSyncStatus) {
                    SyncStatus.Syncing -> stringResource(R.string.sync_status_syncing)
                    SyncStatus.Success -> stringResource(R.string.sync_status_success)
                    SyncStatus.Error -> stringResource(R.string.sync_status_error)
                    else -> ""
                },
                color = Color.White,
                fontSize = 16.sp,  // Чуть крупнее для лучшей читаемости
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ImportWorkoutDropdownMenuItem(viewModel: MyWorkoutsViewModel, onClick: () -> Unit) {
    val context = LocalContext.current
    val importWorkoutSuccessText = stringResource(R.string.import_workout_success)
    val fileNotSelectedText = stringResource(R.string.file_not_selected)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Импортируем тренировку из выбранного файла
                viewModel.importWorkouts(it)
                Toast.makeText(context, importWorkoutSuccessText, Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(context, fileNotSelectedText, Toast.LENGTH_SHORT).show()
            onClick()
        }
    )
    DropdownMenuItem(onClick = {
        importLauncher.launch(arrayOf("application/gpx+xml", "application/octet-stream", "text/xml"))
    }, text = {
        Text(stringResource(R.string.import_workout))
    })
}
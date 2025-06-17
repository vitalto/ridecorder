package ru.ridecorder.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.SpaceAround
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.di.ContextResourceProvider
import ru.ridecorder.ui.common.LoadingScreen
import ru.ridecorder.ui.common.MapScreen
import ru.ridecorder.ui.helpers.ConvertHelper
import ru.ridecorder.ui.helpers.RequestLocationPermissionIfNeeded

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onStopClick: (Long) -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf<Boolean?>(null) }
    var showPermissionRequest by remember { mutableStateOf(true) }

    if (showPermissionRequest) {
        RequestLocationPermissionIfNeeded(
            onPermissionGranted = {
                permissionGranted = true
                showPermissionRequest = false
            },
            onPermissionDenied = {
                permissionGranted = false
                showPermissionRequest = false
            }
        )
    }

    when (permissionGranted) {
        null -> {
            // Пока идет проверка, показываем анимацию загрузки
            LoadingScreen()
        }
        true -> {
            // Показываем контент записи
            RecordingContent(
                viewModel = viewModel,
                onStopClick = onStopClick,
                onExitClick = onExitClick,
                onSettingsClick = onSettingsClick
            )
        }
        false -> {
            // Если разрешение отклонено — сразу уходим
            LaunchedEffect(Unit) {
                onExitClick()
            }
        }
    }
}

@Composable
fun RecordingContent(
    viewModel: RecordingViewModel,
    onStopClick: (Long) -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val trainingState by viewModel.trainingState.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val showResumeDialog by viewModel.showResumeDialog.collectAsState()
    val isBadAccuracy by viewModel.isBadAccuracy.collectAsState()
    val currentHeartRate by viewModel.currentHeartRate.collectAsState(initial = 0)

    // Запускаем проверку незаконченного маршрута при появлении экрана
    LaunchedEffect(Unit) {
        viewModel.checkUnfinishedWorkout()
    }

    // Если найден незаконченный маршрут, показываем диалог
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { /* Не закрывать диалог при клике вне его */ },
            title = { Text(stringResource(id = R.string.resume_dialog_title)) },
            text = { Text(stringResource(id = R.string.resume_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onResumeDialogResult(true, context) }) {
                    Text(stringResource(id = R.string.resume_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onResumeDialogResult(false, context) }) {
                    Text(stringResource(id = R.string.resume_dialog_dismiss))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
    ){
        // Основная колония (вертикальный контейнер)
        Column{
            // Верхняя панель: Скрыть | Заезд | Настройки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.record_hide),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            onExitClick()
                        }
                )
                Text(
                    text = stringResource(id = R.string.record_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = {
                        onSettingsClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.record_settings)
                    )
                }
            }

            // Центральная часть экрана (например, карта)
            Box(
                modifier = Modifier
                    .weight(1f) // Заполняем всё доступное пространство по вертикали
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MapScreen(points = routePoints, badAccuracy = isBadAccuracy)
            }

            // Нижняя часть экрана
            // 1. Отображаем время, дистанцию и скорость
            if(trainingState != TrainingState.Idle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.current_speed),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp, color = Color.Gray)
                        Text(text = ConvertHelper.formatSpeed(ContextResourceProvider(context),
                            stats.currentSpeed),
                            style = MaterialTheme.typography.bodyMedium, fontSize = 18.sp)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Колонка "Время"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.time), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, color = Color.Gray)
                        Text(text = ConvertHelper.formatDuration(
                            ContextResourceProvider(context), viewModel.getDuration()
                        ),
                            style = MaterialTheme.typography.bodyMedium, fontSize = 18.sp)
                    }
                    // Колонка "Дистанция"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.distance), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, color = Color.Gray)
                        Text(text = ConvertHelper.formatDistance(ContextResourceProvider(context), stats.distance),
                            style = MaterialTheme.typography.bodyMedium, fontSize = 18.sp)
                    }
                    // Колонка "Ср. скорость"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(id = R.string.avg_speed), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, color = Color.Gray)
                        Text(text = ConvertHelper.formatSpeed(ContextResourceProvider(context), stats.averageSpeed), style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp)
                    }
                }
            }

            // 2. Кнопки "Стоп", "Финиш" и иконка "Где я"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(trainingState == TrainingState.Idle) {
                    Button(
                        onClick = {
                            viewModel.start(context)
                        },
                        modifier = Modifier
                            .height(70.dp)
                            .width(150.dp),
                        shape = CircleShape
                    ) {
                        Text(text = stringResource(id = R.string.start),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                else {
                    Button(
                        modifier = Modifier.height(56.dp),
                        onClick = {
                            viewModel.pauseToggle(context)
                        }
                    ) {
                        if(trainingState == TrainingState.Pause)
                            Text(text = stringResource(id = R.string.resume), color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                        else
                            Text(text = stringResource(id = R.string.pause), color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                    }
                    Button(
                        modifier = Modifier.height(56.dp),
                        onClick = {
                            if(!viewModel.deleteWorkoutIfEmpty(context)){
                                viewModel.pause(context)
                                onStopClick(viewModel.currentWorkoutId)
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.finish), color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
                    }
                }
            }
        }
        if(currentHeartRate > 0){
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp) // Внешний отступ (margin)
            ) {
                // Блок с фоном и внутренним отступом
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp) // Внутренний отступ (padding)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(id = R.string.heart_rate),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentHeartRate.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

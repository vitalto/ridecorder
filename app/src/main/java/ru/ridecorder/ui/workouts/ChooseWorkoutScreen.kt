package ru.ridecorder.ui.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.ui.common.FeedWorkoutItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseWorkoutScreen(
    onWorkoutSelected: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ChooseWorkoutViewModel = hiltViewModel()
) {
    val workoutList by viewModel.workoutList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.choose_workout_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Допустим, workoutList может быть списком или null, либо можно
        // сделать дополнительную обработку Loading/Error и т.д.
        if (workoutList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(stringResource(id = R.string.no_available_workouts))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workoutList) { workout ->
                    FeedWorkoutItem(
                        workout = viewModel.workoutToDto(workout),
                        onLikeClick = {
                        },
                        onItemClick = {
                            onWorkoutSelected(workout.id)
                        }
                    )
                }
            }
        }
    }
}

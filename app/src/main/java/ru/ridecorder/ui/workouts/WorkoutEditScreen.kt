package ru.ridecorder.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R
import ru.ridecorder.domain.model.RideType
import ru.ridecorder.domain.model.Visibility
import ru.ridecorder.ui.common.LoadingScreen
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditScreen(
    viewModel: WorkoutEditViewModel = hiltViewModel(),
    workoutId: Long,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val workoutState = viewModel.workout.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(workoutState.value!!)
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete_workout))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (workoutState.value == null) {
            LoadingScreen(Modifier.padding(innerPadding))
        } else {
            val workout = workoutState.value!!

            var title by remember { mutableStateOf(workout.name ?: "") }
            var description by remember { mutableStateOf(workout.description ?: "") }
            var selectedVisibility by remember { mutableStateOf(Visibility.fromString(workout.whoCanView)) }
            var selectedRideType by remember { mutableStateOf(RideType.fromString(workout.type)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Название
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.workout_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Описание
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.workout_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Выбор видимости (Visibility)
                Text(text = stringResource(R.string.visibility_label), style = MaterialTheme.typography.titleMedium)
                VisibilityDropdown(
                    selectedVisibility = selectedVisibility,
                    onVisibilitySelected = { selectedVisibility = it }
                )

                // Выбор типа поездки (RideType)
                Text(text = stringResource(R.string.ride_type_label), style = MaterialTheme.typography.titleMedium)
                RideTypeDropdown(
                    selectedRideType = selectedRideType,
                    onRideTypeSelected = { selectedRideType = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка "Удалить тренировку"
                    Button(
                        onClick = {
                            showDeleteDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_workout))
                    }

                    // Кнопка "Сохранить"
                    Button(
                        onClick = {
                            viewModel.save(
                                workout.copy(
                                    name = title,
                                    description = description,
                                    whoCanView = selectedVisibility.value,
                                    type = selectedRideType.value,
                                    updatedAt = Instant.now()
                                )
                            )
                            onBack()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisibilityDropdown(
    selectedVisibility: Visibility,
    onVisibilitySelected: (Visibility) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = Visibility.getDisplayName(selectedVisibility),
            onValueChange = {},
            label = { Text(stringResource(R.string.visibility_label)) },
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.dropdown_icon_description)
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Visibility.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(Visibility.getDisplayName(item)) },
                    onClick = {
                        onVisibilitySelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTypeDropdown(
    selectedRideType: RideType,
    onRideTypeSelected: (RideType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = RideType.getDisplayName(selectedRideType),
            onValueChange = {},
            label = { Text(stringResource(R.string.ride_type_label)) },
            readOnly = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.dropdown_icon_description)
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RideType.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(RideType.getDisplayName(item)) },
                    onClick = {
                        onRideTypeSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

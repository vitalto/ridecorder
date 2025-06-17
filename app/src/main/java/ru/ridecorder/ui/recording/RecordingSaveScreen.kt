package ru.ridecorder.ui.recording

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.ridecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSaveScreen(
    viewModel: RecordingSaveViewModel = hiltViewModel(),
    workoutId: Long,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    // Состояния для "Тип заезда"
    val rideTypeExpanded = remember { mutableStateOf(false) }

    // Состояния для "Кто может просматривать"
    val whoCanViewExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                title = { Text(stringResource(id = R.string.recording_save_title)) },
                actions = {
                    TextButton(onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.saveWorkout(workoutId)
                            onSaveClick()
                        }
                    }) {
                        Text(text = stringResource(id = R.string.recording_save_button_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Поле для ввода названия велозаезда
                OutlinedTextField(
                    value = viewModel.rideName.value,
                    onValueChange = { viewModel.rideName.value = it },
                    placeholder = { Text(stringResource(id = R.string.ride_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Поле для описания (как прошла тренировка)
                OutlinedTextField(
                    value = viewModel.rideDescription.value,
                    onValueChange = { viewModel.rideDescription.value = it },
                    placeholder = { Text(stringResource(id = R.string.ride_description_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Тип заезда (ComboBox)
                ExposedDropdownMenuBox(
                    expanded = rideTypeExpanded.value,
                    onExpandedChange = { rideTypeExpanded.value = !rideTypeExpanded.value }
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedRideType.value.displayName,
                        onValueChange = { /* Ничего не делаем, управляем через меню */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true), // Обязательный модификатор для ExposedDropdownMenu
                        label = { Text(stringResource(id = R.string.ride_type_label)) },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = rideTypeExpanded.value)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = rideTypeExpanded.value,
                        onDismissRequest = { rideTypeExpanded.value = false }
                    ) {
                        viewModel.rideTypeList.forEach { rideType ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(rideType.displayName) },
                                onClick = {
                                    viewModel.selectedRideType.value = rideType
                                    rideTypeExpanded.value = false
                                }
                            )
                        }
                    }
                }

                // Кто может просматривать (ComboBox)
                ExposedDropdownMenuBox(
                    expanded = whoCanViewExpanded.value,
                    onExpandedChange = { whoCanViewExpanded.value = !whoCanViewExpanded.value }
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedWhoCanView.value.displayName,
                        onValueChange = { /* Ничего не делаем, управляем через меню */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        label = { Text(stringResource(id = R.string.who_can_view_label)) },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = whoCanViewExpanded.value)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = whoCanViewExpanded.value,
                        onDismissRequest = { whoCanViewExpanded.value = false }
                    ) {
                        viewModel.whoCanViewList.forEach { whoCanViewOption ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(whoCanViewOption.displayName) },
                                onClick = {
                                    viewModel.selectedWhoCanView.value = whoCanViewOption
                                    whoCanViewExpanded.value = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Кнопка "Удалить тренировку"
                OutlinedButton(
                    onClick = {
                        viewModel.deleteWorkout(workoutId)
                        onDeleteClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text(stringResource(id = R.string.delete_workout))
                }
            }
        }
    }
}

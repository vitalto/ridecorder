package ru.ridecorder.ui.recording

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ru.ridecorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSettingsScreen(
    viewModel: RecordingSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Определяем необходимые разрешения в зависимости от версии Android
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Проверка, что все разрешения предоставлены
    val arePermissionsGranted = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Лаунчер для запроса разрешений
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // После запроса можно обработать результат, если нужно.
        // Здесь можно, например, запустить сканирование, если все разрешения получены.
    }

    val pauseOnIdle by viewModel.pauseOnIdle.collectAsState()

    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.recording_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Настройка паузы на бездействие
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(id = R.string.pause_on_idle_label)) },
                        trailingContent = {
                            Switch(
                                checked = pauseOnIdle,
                                onCheckedChange = { viewModel.setPauseOnIdle(it) }
                            )
                        }
                    )
                }

                // Выбор устройства пульсометра
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(id = R.string.selected_device_label)) },
                        supportingContent = { Text(selectedDevice ?: stringResource(id = R.string.device_not_selected)) },
                        trailingContent = {
                            Button(onClick = {
                                if (arePermissionsGranted) {
                                    viewModel.startScan()
                                    showDeviceDialog = true
                                } else {
                                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                                }
                            }) {
                                Text(stringResource(id = R.string.scan))
                            }
                        }
                    )
                }
            }
        }

        // Диалог выбора устройства
        if (showDeviceDialog) {
            AlertDialog(
                onDismissRequest = { showDeviceDialog = false },
                title = { Text(stringResource(id = R.string.device_select_title)) },
                text = {
                    if(!arePermissionsGranted){
                        Text(stringResource(id = R.string.device_scan_no_permission))
                    }
                    else if (scannedDevices.isEmpty()) {
                        Text(stringResource(id = R.string.device_scanning))
                    } else {
                        LazyColumn {
                            items(scannedDevices) { device: BluetoothDevice ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSelectedDevice(device.address)
                                            viewModel.stopScan()
                                            showDeviceDialog = false
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(text = device.name ?: stringResource(id = R.string.unknown_device))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "(${device.address})")
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.stopScan()
                        showDeviceDialog = false
                    }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                },
                confirmButton = {}
            )
        }
    }
}

package ru.ridecorder.ui.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.ridecorder.R

@Composable
fun RequestLocationPermissionIfNeeded(
    onPermissionGranted:() -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var showNeverAskAgainDialog by remember { mutableStateOf(false) }
    var settingsOpened by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            if (activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Пользователь выбрал "Никогда не спрашивать снова" (или система так решила)
                // Покажем диалог с предложением перейти в настройки
                showNeverAskAgainDialog = true
            } else {
                // Обычный отказ (без "Не спрашивать снова")
                onPermissionDenied()
            }
        }
    }

    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            onPermissionGranted()
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Наблюдаем за жизненным циклом, чтобы проверить разрешение после возвращения из настроек
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settingsOpened) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && settingsOpened) {
                // Проверяем состояние разрешения при возврате
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (permissionGranted) {
                    showNeverAskAgainDialog = false
                    settingsOpened = false
                    onPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Если пользователь выбрал "Никогда не спрашивать снова", показываем свой диалог
    if (showNeverAskAgainDialog) {
        NeverAskAgainDialog(
            onGoToSettings = {
                // Открываем настройки приложения
                openAppSettings(context)
                settingsOpened = true
            },
            onDismiss = {
                // Если пользователь передумал, просто закрываем диалог и вызываем onPermissionDenied
                showNeverAskAgainDialog = false
                onPermissionDenied()
            }
        )
    }
}

@Composable
fun NeverAskAgainDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.permission_required_title)) },
        text = {
            Text(
                text = stringResource(id = R.string.permission_required_message)
            )
        },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text(text = stringResource(id = R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
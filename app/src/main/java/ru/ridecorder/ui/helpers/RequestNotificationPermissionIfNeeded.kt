package ru.ridecorder.ui.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


// Код запроса, используемый при callback'е в onRequestPermissionsResult()
const val REQUEST_CODE_POST_NOTIFICATIONS = 101

/**
 * Запрашивает разрешение POST_NOTIFICATIONS, если оно не выдано и устройство работает на Android 13+.
 */
fun requestNotificationPermissionIfNeeded(activity: Activity) {
    // Проверяем версию Android
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Проверяем, дано ли уже разрешение
        val isGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            // Если не дано - запрашиваем
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }
}

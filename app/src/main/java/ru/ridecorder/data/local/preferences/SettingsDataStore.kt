package ru.ridecorder.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsDataStore(private val context: Context) {
    companion object {
        private val PAUSE_ON_IDLE_KEY = booleanPreferencesKey("pause_on_idle")
        private val SELECTED_HRM_DEVICE = stringPreferencesKey("selected_hrm_device")
    }

    val pauseOnIdleFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[PAUSE_ON_IDLE_KEY] ?: false }

    val selectedDeviceFlow: Flow<String> = context.settingsDataStore.data
        .map { preferences -> preferences[SELECTED_HRM_DEVICE] ?: "" }

    suspend fun setPauseOnIdle(value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PAUSE_ON_IDLE_KEY] = value
        }
    }

    suspend fun setHRMDevice(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[SELECTED_HRM_DEVICE] = value
        }
    }
}

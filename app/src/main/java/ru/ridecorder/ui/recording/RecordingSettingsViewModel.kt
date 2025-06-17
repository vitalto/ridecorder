package ru.ridecorder.ui.recording

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.ridecorder.data.local.preferences.SettingsDataStore
import javax.inject.Inject

@HiltViewModel
class RecordingSettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val bluetoothAdapter: BluetoothAdapter
) : ViewModel() {

    private val _pauseOnIdle = MutableStateFlow(false)
    val pauseOnIdle: StateFlow<Boolean> = _pauseOnIdle

    private val _selectedDevice = MutableStateFlow<String?>(null)
    val selectedDevice: StateFlow<String?> = _selectedDevice

    // Поток найденных устройств
    private val _scannedDevices = MutableStateFlow<List<android.bluetooth.BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<android.bluetooth.BluetoothDevice>> = _scannedDevices

    // Для сканирования устройств
    private var scanCallback: ScanCallback? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.pauseOnIdleFlow.collect { value ->
                _pauseOnIdle.value = value
            }
        }
        viewModelScope.launch {
            settingsDataStore.selectedDeviceFlow.collect { device ->
                _selectedDevice.value = device
            }
        }
    }

    fun setPauseOnIdle(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPauseOnIdle(value)
        }
    }

    fun setSelectedDevice(device: String) {
        viewModelScope.launch {
            settingsDataStore.setHRMDevice(device)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        _scannedDevices.value = emptyList() // очистка предыдущих результатов

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.device?.let { device ->
                    if (_scannedDevices.value.none { it.address == device.address }) {
                        _scannedDevices.value += device
                    }
                }
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    result.device?.let { device ->
                        if (_scannedDevices.value.none { it.address == device.address }) {
                            _scannedDevices.value += device
                        }
                    }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e("RecordingSettingsVM", "Scan failed: $errorCode")
            }
        }
        bluetoothLeScanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

}
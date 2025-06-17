package ru.ridecorder.data.local

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

class HeartRateMonitorManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {

    companion object {
        private val HEART_RATE_SERVICE_UUID: UUID =
            UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID: UUID =
            UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null

    // Flow для передачи значений пульса
    private val _heartRateFlow = MutableSharedFlow<Int>(replay = 1)
    val heartRateFlow = _heartRateFlow.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("HeartRateMonitor", "Connected, starting service discovery")

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("HeartRateMonitor", "No permissions")
                    return
                }

                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("HeartRateMonitor", "Disconnected from device")
                _heartRateFlow.tryEmit(0)
            }
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("HeartRateMonitor", "No permissions")
                return
            }

            val service = gatt.getService(HEART_RATE_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            characteristic?.let {
                // Включаем уведомления для характеристики измерения пульса
                gatt.setCharacteristicNotification(it, true)
                val descriptor = it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                // Подавляем предупреждение об устаревшем API, альтернативного метода пока нет
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                val flag = characteristic.properties
                var format = BluetoothGattCharacteristic.FORMAT_UINT8
                if (flag and 0x01 != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16
                }
                val heartRate = characteristic.getIntValue(format, 1)
                _heartRateFlow.tryEmit(heartRate)
            }
        }
    }

    /**
     * Метод вызывается при условии, что разрешения уже проверены (например, в Activity).
     * Если разрешения не проверены, то перед вызовом этой функции необходимо выполнить проверку.
     */

    fun connectToDevice(deviceAddress: String) {
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }


    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothGatt?.disconnect()
        bluetoothGatt = null
    }

}

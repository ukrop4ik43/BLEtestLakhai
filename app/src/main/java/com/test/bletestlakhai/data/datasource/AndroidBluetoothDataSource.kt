package com.test.bletestlakhai.data.datasource

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.test.bletestlakhai.domain.model.BleDevice
import com.test.bletestlakhai.domain.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "AndroidBluetoothDS"

@SuppressLint("MissingPermission") // Дозволи перевіряються вище
class AndroidBluetoothDataSource(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")

    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var targetDevice: BluetoothDevice? = null

    private val _connectionStatusFlow =
        MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatusFlow: Flow<ConnectionStatus> = _connectionStatusFlow.asStateFlow()

    private val _discoveredServicesFlow = MutableStateFlow<List<String>>(emptyList())
    val discoveredServicesFlow: Flow<List<String>> = _discoveredServicesFlow.asStateFlow()

    private val _rssiFlow = MutableSharedFlow<Int>() // Raw RSSI
    val rssiFlow: Flow<Int> = _rssiFlow.asSharedFlow()

    private val _characteristicValueFlow = MutableSharedFlow<Pair<UUID, ByteArray>>()
    val characteristicValueFlow: Flow<Pair<UUID, ByteArray>> =
        _characteristicValueFlow.asSharedFlow()


    private var rssiReadJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Connected to $deviceAddress")
                        this@AndroidBluetoothDataSource.gatt = gatt
                        _connectionStatusFlow.update { ConnectionStatus.Connected(deviceAddress) }
                        // Запускаємо виявлення сервісів після успішного підключення
                        Handler(Looper.getMainLooper()).postDelayed({
                            gatt.discoverServices()
                        }, 500) // Невелика затримка може допомогти на деяких пристроях
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Disconnected from $deviceAddress")
                        _connectionStatusFlow.update { ConnectionStatus.Disconnected }
                        cleanupGatt()
                    }
                }
            } else {
                Log.e(TAG, "Connection state change error for $deviceAddress: $status")
                _connectionStatusFlow.update { ConnectionStatus.Error("Connection failed with status: $status") }
                cleanupGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered for ${gatt.device.address}")
                _discoveredServicesFlow.value = gatt.services.map { it.uuid.toString() }
                _connectionStatusFlow.update { ConnectionStatus.ServicesDiscovered }
                // Можна автоматично почати читати RSSI тут, якщо потрібно
                // startRssiPolling()
            } else {
                Log.w(TAG, "Service discovery failed with status: $status")
                _connectionStatusFlow.update { ConnectionStatus.Error("Service discovery failed") }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Remote RSSI: $rssi dBm")
                coroutineScope.launch {
                    _rssiFlow.emit(rssi)
                }
            } else {
                Log.w(TAG, "Failed to read RSSI, status: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(
                TAG,
                "Characteristic ${characteristic.uuid} changed: ${characteristic.value.joinToString()}"
            )
            coroutineScope.launch {
                _characteristicValueFlow.emit(characteristic.uuid to characteristic.value.copyOf())
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.i(
                TAG,
                "Characteristic ${characteristic.uuid} changed (new API): ${value.joinToString()}"
            )
            coroutineScope.launch {
                _characteristicValueFlow.emit(characteristic.uuid to value.copyOf())
            }
        }


        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Characteristic ${characteristic.uuid} read: ${value.joinToString()}")
                coroutineScope.launch {
                    _characteristicValueFlow.emit(characteristic.uuid to value.copyOf())
                }
            } else {
                Log.w(TAG, "Failed to read characteristic ${characteristic.uuid}, status: $status")
            }
        }
    }

    fun scanForDevice(targetDeviceAddress: String): Flow<BleDevice?> = callbackFlow {
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.d(TAG, "Scan result: ${result.device.address} - ${result.device.name ?: "N/A"}")
                if (result.device.address == targetDeviceAddress) {
                    targetDevice = result.device
                    trySend(BleDevice(result.device.address, result.device.name))
                    close()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
                close(IllegalStateException("Scan failed with error: $errorCode"))
            }
        }

        Log.i(TAG, "Starting BLE scan for $targetDeviceAddress")
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)

        awaitClose {
            Log.i(TAG, "Stopping BLE scan")
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    fun connect(deviceAddress: String) {
        if (!bluetoothAdapter.isEnabled) {
            _connectionStatusFlow.update { ConnectionStatus.Error("Bluetooth is not enabled") }
            return
        }
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            _connectionStatusFlow.update { ConnectionStatus.Error("Device not found") }
            return
        }
        targetDevice = device
        _connectionStatusFlow.update { ConnectionStatus.Connecting }
        Log.i(TAG, "Connecting to ${device.address}")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun discoverServicesOnDevice() {
        gatt?.discoverServices() ?: Log.w(TAG, "GATT not initialized, cannot discover services")
    }

    fun startRssiPolling(intervalMillis: Long = 2000) {
        rssiReadJob?.cancel()
        rssiReadJob = coroutineScope.launch {
            while (true) {
                gatt?.readRemoteRssi()
                delay(intervalMillis)
            }
        }
    }

    fun stopRssiPolling() {
        rssiReadJob?.cancel()
        rssiReadJob = null
    }

    fun readCharacteristicFromDevice(
        serviceUuidString: String,
        characteristicUuidString: String
    ): Boolean {
        val serviceUuid = UUID.fromString(serviceUuidString)
        val characteristicUuid = UUID.fromString(characteristicUuidString)

        val service = gatt?.getService(serviceUuid)
        if (service == null) {
            Log.w(TAG, "Service $serviceUuidString not found.")
            return false
        }
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Log.w(
                TAG,
                "Characteristic $characteristicUuidString not found in service $serviceUuidString."
            )
            return false
        }
        return gatt?.readCharacteristic(characteristic) ?: false
    }


    fun enableCharacteristicNotificationsForDevice(
        serviceUuidString: String,
        characteristicUuidString: String
    ): Boolean {
        val serviceUuid = UUID.fromString(serviceUuidString)
        val characteristicUuid = UUID.fromString(characteristicUuidString)

        val service = gatt?.getService(serviceUuid)
        if (service == null) {
            Log.w(TAG, "Service $serviceUuidString not found for notification.")
            return false
        }
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Log.w(TAG, "Characteristic $characteristicUuidString not found for notification.")
            return false
        }

        val cccdUuid =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor == null) {
            Log.w(TAG, "CCCD not found for characteristic $characteristicUuidString.")
            return false
        }

        val notificationEnabled = gatt?.setCharacteristicNotification(characteristic, true) ?: false
        if (!notificationEnabled) {
            Log.w(TAG, "Failed to enable notification for $characteristicUuidString.")
            return false
        }


        val success = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if (!success) {
            Log.w(TAG, "Failed to set descriptor value for notification.")
            return false
        }
        return gatt?.writeDescriptor(descriptor) ?: false
    }


    fun disconnectGatt() {
        stopRssiPolling()
        gatt?.disconnect()
    }

    fun cleanupGatt() {
        stopRssiPolling()
        gatt?.close()
        gatt = null
        targetDevice = null
        _discoveredServicesFlow.value = emptyList()
        if (_connectionStatusFlow.value !is ConnectionStatus.Error) {
            _connectionStatusFlow.update { ConnectionStatus.Disconnected }
        }
    }
}
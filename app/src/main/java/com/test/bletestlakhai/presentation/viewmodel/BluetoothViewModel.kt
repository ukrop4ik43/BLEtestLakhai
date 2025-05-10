package com.test.bletestlakhai.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.bletestlakhai.data.datasource.AndroidBluetoothDataSource
import com.test.bletestlakhai.data.repository.BluetoothRepositoryImpl
import com.test.bletestlakhai.domain.model.BleDevice
import com.test.bletestlakhai.domain.model.ConnectionStatus
import com.test.bletestlakhai.domain.model.RssiData
import com.test.bletestlakhai.domain.repository.BluetoothRepository
import com.test.bletestlakhai.domain.usecase.CalculateDistanceUseCase
import com.test.bletestlakhai.domain.usecase.FilterRssiUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG_VM = "BluetoothViewModel"
private val HM10_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
private val HM10_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"


class BluetoothViewModel(
    private val filterRssiUseCase: FilterRssiUseCase,
    private val repository: BluetoothRepository
) : ViewModel() {

    private val _targetDeviceAddress = MutableStateFlow("48:87:2D:9C:F5:42") // Ваша цільова адреса
    val targetDeviceAddress: StateFlow<String> = _targetDeviceAddress.asStateFlow()

    private val _scannedDevice = MutableStateFlow<BleDevice?>(null)
    val scannedDevice: StateFlow<BleDevice?> = _scannedDevice.asStateFlow()

    private val _connectionStatus =
        MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _rssiData = MutableStateFlow<RssiData?>(null)
    val rssiData: StateFlow<RssiData?> = _rssiData.asStateFlow()

    private val _discoveredServices = MutableStateFlow<List<String>>(emptyList())
    val discoveredServices: StateFlow<List<String>> = _discoveredServices.asStateFlow()

    private val _gpsCharacteristicData = MutableStateFlow<String?>(null)
    val gpsCharacteristicData: StateFlow<String?> = _gpsCharacteristicData.asStateFlow()


    private var scanJob: Job? = null
    private var connectionJob: Job? = null
    private var rssiJob: Job? = null
    private var servicesJob: Job? = null
    private var charNotificationJob: Job? = null


    fun setTargetDeviceAddress(address: String) {
        if (_targetDeviceAddress.value != address) {
            _targetDeviceAddress.value = address
            if (_connectionStatus.value is ConnectionStatus.Connected || _connectionStatus.value is ConnectionStatus.Connecting) {
                disconnectDevice()
            }
            clearPreviousDeviceData()
        }
    }

    private fun clearPreviousDeviceData() {
        _scannedDevice.value = null
        _rssiData.value = null
        _discoveredServices.value = emptyList()
        _gpsCharacteristicData.value = null
        filterRssiUseCase.reset()
    }


    fun startScanAndConnect() {
        if (scanJob?.isActive == true || connectionJob?.isActive == true) {
            Log.d(TAG_VM, "Scan or connection already in progress.")
            return
        }
        clearPreviousDeviceData()
        _connectionStatus.value = ConnectionStatus.Connecting

        scanJob = viewModelScope.launch {
            Log.i(TAG_VM, "Starting scan for ${_targetDeviceAddress.value}")
            repository.scanForDevice(_targetDeviceAddress.value)
                .catch { e ->
                    Log.e(TAG_VM, "Scan error: ${e.message}")
                    _connectionStatus.value = ConnectionStatus.Error("Scan failed: ${e.message}")
                }
                .collect { device ->
                    if (device != null) {
                        _scannedDevice.value = device
                        Log.i(TAG_VM, "Device found: ${device.address}. Attempting to connect.")
                        connectToDevice(device.address)
                        scanJob?.cancel() // Зупинити сканування після знаходження
                    } else {
                        Log.w(TAG_VM, "Target device not found during scan.")
                    }
                }
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            repository.connect(deviceAddress)
                .catch { e ->
                    Log.e(TAG_VM, "Connection flow error: ${e.message}")
                    _connectionStatus.value =
                        ConnectionStatus.Error("Connection error: ${e.message}")
                }
                .collect { status ->
                    _connectionStatus.value = status
                    when (status) {
                        is ConnectionStatus.Connected -> {
                            Log.i(
                                TAG_VM,
                                "Successfully connected to $deviceAddress. Discovering services."
                            )
                            discoverDeviceServices()
                        }

                        is ConnectionStatus.ServicesDiscovered -> {
                            Log.i(
                                TAG_VM,
                                "Services discovered. Starting RSSI updates and characteristic notifications."
                            )
                            startRssiUpdatesForDevice()
                            enableGpsCharacteristicNotifications()
                        }

                        is ConnectionStatus.Error -> Log.e(
                            TAG_VM,
                            "Connection error: ${status.message}"
                        )

                        ConnectionStatus.Disconnected -> Log.i(TAG_VM, "Device disconnected.")
                        ConnectionStatus.Connecting -> Log.i(TAG_VM, "Device connecting...")
                    }
                }
        }
    }

    private fun discoverDeviceServices() {
        servicesJob?.cancel()
        servicesJob = viewModelScope.launch {
            Log.d(TAG_VM, "Requesting service discovery.")
            repository.discoverServices()
                .catch { e -> Log.e(TAG_VM, "Error discovering services: ${e.message}") }
                .collect { services ->
                    _discoveredServices.value = services
                    Log.i(TAG_VM, "Discovered services: $services")
                }
        }
    }


    private fun startRssiUpdatesForDevice() {
        rssiJob?.cancel()
        rssiJob = viewModelScope.launch {
            Log.d(TAG_VM, "Starting RSSI updates.")
            repository.startRssiUpdates()
                .catch { e -> Log.e(TAG_VM, "Error in RSSI flow: ${e.message}") }
                .collect { data ->
                    _rssiData.value = data
                    Log.d(
                        TAG_VM,
                        "RSSI Data: Raw=${data.rawRssi}, Filtered=${"%.2f".format(data.filteredRssi)}, Dist=${
                            "%.2f".format(data.distance)
                        }m"
                    )
                }
        }
    }

    private fun enableGpsCharacteristicNotifications() {
        charNotificationJob?.cancel()
        charNotificationJob = viewModelScope.launch {
            Log.d(
                TAG_VM,
                "Enabling notifications for GPS characteristic: $HM10_SERVICE_UUID / $HM10_CHARACTERISTIC_UUID"
            )
            repository.enableCharacteristicNotifications(
                HM10_SERVICE_UUID,
                HM10_CHARACTERISTIC_UUID
            )
                .catch { e -> Log.e(TAG_VM, "Error enabling GPS notifications: ${e.message}") }
                .collect { byteArray ->
                    val dataString = byteArray.toString(Charsets.UTF_8) // Або інше кодування
                    _gpsCharacteristicData.value = dataString
                    Log.i(
                        TAG_VM,
                        "Received GPS Data via notification: $dataString (Bytes: ${byteArray.joinToString()})"
                    )
                }
        }
    }


    fun disconnectDevice() {
        Log.i(TAG_VM, "Disconnecting device.")
        scanJob?.cancel()
        connectionJob?.cancel()
        rssiJob?.cancel()
        servicesJob?.cancel()
        charNotificationJob?.cancel()
        repository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG_VM, "ViewModel cleared. Cleaning up repository.")
        repository.cleanup()
    }
}
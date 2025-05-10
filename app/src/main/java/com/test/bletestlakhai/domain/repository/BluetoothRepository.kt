package com.test.bletestlakhai.domain.repository


import com.test.bletestlakhai.domain.model.BleDevice
import com.test.bletestlakhai.domain.model.ConnectionStatus
import com.test.bletestlakhai.domain.model.RssiData
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    fun scanForDevice(targetDeviceAddress: String): Flow<BleDevice?>
    fun connect(deviceAddress: String): Flow<ConnectionStatus>
    fun startRssiUpdates(): Flow<RssiData>
    fun discoverServices(): Flow<List<String>>
    fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray>
    fun enableCharacteristicNotifications(serviceUuid: String, characteristicUuid: String): Flow<ByteArray>
    fun disconnect()
    fun cleanup()
}
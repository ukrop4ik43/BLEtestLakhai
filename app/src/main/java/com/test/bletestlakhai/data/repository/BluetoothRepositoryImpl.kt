package com.test.bletestlakhai.data.repository

import com.test.bletestlakhai.data.datasource.AndroidBluetoothDataSource
import com.test.bletestlakhai.domain.model.BleDevice
import com.test.bletestlakhai.domain.model.ConnectionStatus
import com.test.bletestlakhai.domain.model.RssiData
import com.test.bletestlakhai.domain.repository.BluetoothRepository
import com.test.bletestlakhai.domain.usecase.CalculateDistanceUseCase
import com.test.bletestlakhai.domain.usecase.FilterRssiUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

class BluetoothRepositoryImpl(
    private val dataSource: AndroidBluetoothDataSource,
    private val filterRssiUseCase: FilterRssiUseCase,
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) : BluetoothRepository {

    override fun scanForDevice(targetDeviceAddress: String): Flow<BleDevice?> {
        return dataSource.scanForDevice(targetDeviceAddress)
    }

    override fun connect(deviceAddress: String): Flow<ConnectionStatus> {
        filterRssiUseCase.reset()
        dataSource.connect(deviceAddress)
        return dataSource.connectionStatusFlow
    }

    override fun discoverServices(): Flow<List<String>> {
        dataSource.discoverServicesOnDevice()
        return dataSource.discoveredServicesFlow
    }


    override fun startRssiUpdates(): Flow<RssiData> {
        dataSource.startRssiPolling()
        return dataSource.rssiFlow.map { rawRssi ->
            val filteredRssi = filterRssiUseCase(rawRssi.toDouble())
            val distance = calculateDistanceUseCase(filteredRssi)
            RssiData(rawRssi, filteredRssi, distance)
        }
    }

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        dataSource.readCharacteristicFromDevice(serviceUuid, characteristicUuid)
        return dataSource.characteristicValueFlow.map { it.second }
    }

    override fun enableCharacteristicNotifications(serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        dataSource.enableCharacteristicNotificationsForDevice(serviceUuid, characteristicUuid)
        return dataSource.characteristicValueFlow.filter { it.first == UUID.fromString(characteristicUuid) }.map { it.second }
    }

    override fun disconnect() {
        dataSource.disconnectGatt()
    }

    override fun cleanup() {
        dataSource.cleanupGatt()
    }
}
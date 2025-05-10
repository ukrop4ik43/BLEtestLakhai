package com.test.bletestlakhai.domain.model

sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data object Connecting : ConnectionStatus()
    data class Connected(val deviceAddress: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
    data object ServicesDiscovered : ConnectionStatus()
}
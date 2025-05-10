package com.test.bletestlakhai.presentation.screen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.bletestlakhai.domain.model.ConnectionStatus
import com.test.bletestlakhai.presentation.viewmodel.BluetoothViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BleDataScreen(
    viewModel: BluetoothViewModel = koinViewModel(),
    onGrantPermissionClick: () -> Unit,
    hasPermissions: Boolean
) {
    val targetAddress by viewModel.targetDeviceAddress.collectAsState()
    val scannedDevice by viewModel.scannedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val rssiData by viewModel.rssiData.collectAsState()
    val discoveredServices by viewModel.discoveredServices.collectAsState()
    val gpsData by viewModel.gpsCharacteristicData.collectAsState()

    var editableTargetAddress by remember { mutableStateOf(targetAddress) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("BLE Data Monitor", style = MaterialTheme.typography.headlineSmall)

        if (!hasPermissions) {
            Button(onClick = onGrantPermissionClick) {
                Text("Grant BLE Permissions")
            }
        } else {
            OutlinedTextField(
                value = editableTargetAddress,
                onValueChange = { editableTargetAddress = it },
                label = { Text("Target Device MAC Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.setTargetDeviceAddress(editableTargetAddress)
                        viewModel.startScanAndConnect()
                    },
                    enabled = connectionStatus !is ConnectionStatus.Connecting && connectionStatus !is ConnectionStatus.Connected
                ) {
                    Text("Scan & Connect")
                }
                Button(
                    onClick = { viewModel.disconnectDevice() },
                    enabled = connectionStatus is ConnectionStatus.Connected || connectionStatus is ConnectionStatus.Connecting || connectionStatus is ConnectionStatus.ServicesDiscovered
                ) {
                    Text("Disconnect")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Target: ${targetAddress}", fontSize = 16.sp)
        Text("Scanned Device: ${scannedDevice?.name ?: scannedDevice?.address ?: "Not found"}", fontSize = 16.sp)

        val statusText = when (val status = connectionStatus) {
            ConnectionStatus.Connecting -> "Status: Connecting..."
            is ConnectionStatus.Connected -> "Status: Connected to ${status.deviceAddress}"
            ConnectionStatus.Disconnected -> "Status: Disconnected"
            is ConnectionStatus.Error -> "Status: Error - ${status.message}"
            ConnectionStatus.ServicesDiscovered -> "Status: Services Discovered"
        }
        Text(statusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        rssiData?.let {
            Text("Raw RSSI: ${it.rawRssi} dBm", fontSize = 16.sp)
            Text("Filtered RSSI: ${"%.2f".format(it.filteredRssi)} dBm", fontSize = 16.sp)
            Text("Estimated Distance: ${"%.2f".format(it.distance)} m", fontSize = 16.sp)
        } ?: Text("RSSI Data: N/A", fontSize = 16.sp)


        Text("Discovered Services (${discoveredServices.size}):", fontWeight = FontWeight.SemiBold)
        if (discoveredServices.isNotEmpty()) {
            discoveredServices.take(3).forEach { serviceUuid -> // Показуємо перші 3
                Text("- $serviceUuid", fontSize = 12.sp)
            }
            if (discoveredServices.size > 3) Text("... and more")
        } else {
            Text("No services discovered yet.", fontSize = 12.sp)
        }

        Text("GPS (FFE1) Data: ${gpsData ?: "N/A"}", fontSize = 16.sp)
    }
}
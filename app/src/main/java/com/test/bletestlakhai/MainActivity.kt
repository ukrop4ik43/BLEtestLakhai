package com.test.bletestlakhai

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.test.bletestlakhai.ui.theme.BLETestLakhaiTheme
import com.test.bletestlakhai.util.GrantPermissionsButton
import java.util.UUID


class MainActivity : ComponentActivity() {
    private lateinit var bluetooth: BluetoothManager
    private var gatt: BluetoothGatt? = null
    private var services: List<BluetoothGattService> = emptyList()

    var selectedDevice: BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    fun discoverServices() {

        gatt?.discoverServices()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BLETestLakhaiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GrantPermissionsButton(
                        innerPadding, { context ->
                            hasBlePermissions(context)
                            Log.d("dsadsadads", "permission granted")
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun hasBlePermissions(context: Context) {
        bluetooth = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
        val scanner: BluetoothLeScanner = bluetooth.adapter.bluetoothLeScanner


        val scanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                //TODO: Set the criteria for selecting the device. Here we check the device's
                // name from the advertisement packet, but you could for example check its
                // manufacturer, address, services, etc.
                Log.d("dsadsadads", "device address " + "${result?.device?.address}")
                if ((result?.device?.address ?: "") == "48:87:2D:9C:F5:42") {
                    //We have found what we're looking for. Save it for later.
                    selectedDevice = result?.device
                    connect(context = context)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d("dsadsadads", "errorCode")
                //TODO: Something went wrong
            }
        }
        scanner.startScan(scanCallback)
    }

    //Whatever we do with our Bluetooth device connection, whether now or later, we will get the
//results in this callback object, which can become massive.
    private val callback = object : BluetoothGattCallback() {
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d("dsadsadads", String(characteristic.value))

        }



        //We will override more methods here as we add functionality.
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d("dsadsadads", "onServicesDiscovered ${gatt.services}")
            services = gatt.services
            Log.d("dsadsadads", "firstService ${services.get(0).uuid}")
            readCharacteristic(services.get(0).uuid,services.get(0).characteristics.get(0).uuid)

        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            //This tells us when we're connected or disconnected from the peripheral.

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("dsadsadads", "error $status")
                return
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("dsadsadads", "CONNECTED!")
                discoverServices()

            }
        }
    }
    @SuppressLint("MissingPermission")
    fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
            Log.v("bluetooth", "Read status: $success")
        }
    }
    @SuppressLint("MissingPermission")
    fun connect(context: Context) {
        gatt = selectedDevice?.connectGatt(context, false, callback)
    }
}



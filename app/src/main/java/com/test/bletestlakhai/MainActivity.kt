package com.test.bletestlakhai

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.test.bletestlakhai.ui.theme.BLETestLakhaiTheme
import com.test.bletestlakhai.util.GrantPermissionsButton
import java.util.UUID


class MainActivity : ComponentActivity() {
    private lateinit var  bluetooth :BluetoothManager





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BLETestLakhaiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GrantPermissionsButton(
                        innerPadding,{
                            hasBlePermissions()
                            Log.d("dsadsadads","permission granted")}
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun hasBlePermissions(){
        bluetooth= applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
         val scanner: BluetoothLeScanner
        = bluetooth.adapter.bluetoothLeScanner
         var selectedDevice: BluetoothDevice? = null

         val scanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                //TODO: Set the criteria for selecting the device. Here we check the device's
                // name from the advertisement packet, but you could for example check its
                // manufacturer, address, services, etc.
                Log.d("dsadsadads","${result?.device?.name}")
                if ((result?.device?.name ?: "") == "MyDevice") {
                    //We have found what we're looking for. Save it for later.
                    selectedDevice = result?.device
                }
            }
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d("dsadsadads","errorCode")
                //TODO: Something went wrong
            }
        }
        scanner.startScan(scanCallback)
    }
}



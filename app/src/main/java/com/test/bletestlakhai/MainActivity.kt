package com.test.bletestlakhai

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.test.bletestlakhai.ui.theme.BLETestLakhaiTheme
import com.test.bletestlakhai.util.REQUEST_ENABLE_BT
import com.test.bletestlakhai.util.hasPermissions
import java.util.UUID


class MainActivity : ComponentActivity() {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val scanner = adapter.bluetoothLeScanner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BLETestLakhaiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }


    }


    @SuppressLint("MissingPermission")
    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        hasPermissions(this@MainActivity,this.applicationContext)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
try{
    if (!bluetoothAdapter.isEnabled) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }
}catch (e:SecurityException){
    Log.d("dsadsadads","security exception")
}

        val scanCallback: ScanCallback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device: BluetoothDevice = result.device
                Log.d("dsadsadads", device.name)
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
               Log.d("dsadsadads","batch scan results")
            }

            override fun onScanFailed(errorCode: Int) {
               Log.d("dsadsadads","scan failed")
            }
        }

        val BLP_SERVICE_UUID = UUID.fromString("00001234-0000-1000-8000-00805F9B34FB")
        val serviceUUIDs = arrayOf(BLP_SERVICE_UUID)
        val filters: MutableList<ScanFilter?>?
        filters = ArrayList()
        for (serviceUUID in serviceUUIDs) {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUUID))
                .build()
            filters.add(filter)
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()
        if (scanner != null) {
            scanner.startScan(filters, scanSettings, scanCallback)
            Log.d("dsadsadads", "scan started")
        } else {
            Log.e("dsadsadads", "could not get scanner object")
        }

        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }
}



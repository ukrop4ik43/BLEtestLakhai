package com.test.bletestlakhai.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import com.test.bletestlakhai.MainActivity


const val REQUEST_ENABLE_BT = 2
fun hasPermissions(activity: Activity, context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ), 1
            )
            return false
        }
    } else { // Below Android 12
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return false
        }
    }
    return true
}

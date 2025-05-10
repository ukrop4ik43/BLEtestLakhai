package com.test.bletestlakhai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.test.bletestlakhai.common.PermissionUtils
import com.test.bletestlakhai.presentation.screen.BleDataScreen
import com.test.bletestlakhai.presentation.viewmodel.BluetoothViewModel
import com.test.bletestlakhai.ui.theme.BLETestLakhaiTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BLETestLakhaiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var hasPermissions by remember { mutableStateOf(PermissionUtils.hasBlePermissions(context)) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        hasPermissions = permissionsMap.values.all { it }
                        if (hasPermissions) {
                            // Дозволи надано, можна ініціювати дії, якщо потрібно
                        } else {
                            // Обробка відмови у дозволах
                        }
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            BleDataScreen(
                                onGrantPermissionClick = {
                                    permissionLauncher.launch(PermissionUtils.ALL_BLE_PERMISSIONS)
                                },
                                hasPermissions = hasPermissions
                            )
                        }
                    }
                }
            }
        }
    }


}



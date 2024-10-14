package com.crakac.blemessaging

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crakac.blemessaging.MainViewModel.ServerState
import com.crakac.blemessaging.ble.BlePermissions
import com.crakac.blemessaging.ble.ScanState
import com.crakac.blemessaging.ble.blePermissionsGranted
import com.crakac.blemessaging.ui.theme.BleMessagingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val blePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.any { it.value == false }) {
                Toast.makeText(this, "BLE permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            BleMessagingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!this@MainActivity.blePermissionsGranted()) {
                            LaunchedEffect(Unit) {
                                blePermissionsLauncher.launch(BlePermissions)
                            }
                        } else {
                            Button(
                                enabled = uiState.serverState !is ServerState.Running,
                                onClick = {
                                    @SuppressLint("MissingPermission")
                                    withBlePermissions {
                                        viewModel.startServer()
                                    }
                                }) {
                                Text("Start Server")
                            }
                            Button(
                                enabled = uiState.scanState != ScanState.Scanning,
                                onClick = {
                                    @SuppressLint("MissingPermission")
                                    withBlePermissions {
                                        viewModel.connect()
                                    }
                                }) {
                                Text("Connect")
                            }
                        }
                        Text("Server state: ${uiState.serverState}")
                        Text("Scan state: ${uiState.scanState}")
                        Text("Connection state: ${uiState.connectionState}")
                    }
                }
            }
        }
    }

    private fun Context.withBlePermissions(block: () -> Unit) {
        if (blePermissionsGranted()) {
            block()
        } else {
            blePermissionsLauncher.launch(BlePermissions)
        }
    }
}


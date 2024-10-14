package com.crakac.blemessaging

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crakac.blemessaging.ble.BlePermissions
import com.crakac.blemessaging.ble.EmobClientState
import com.crakac.blemessaging.ble.ScanState
import com.crakac.blemessaging.ble.model.MyMessage
import com.crakac.blemessaging.ble.model.RemoteMessage
import com.crakac.blemessaging.ui.theme.EmobTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            EmobTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (uiState.clientState is EmobClientState.Connected) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.sendMessage("Hello")
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Default.Send, "send message")
                            }
                        }
                    }
                ) { innerPadding ->
                    BlePermissionsBox(modifier = Modifier.padding(innerPadding)) {
                        LaunchedEffect(Unit) {
                            viewModel.startServer()
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(innerPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row {
                                IconButton(
                                    enabled = !uiState.serverState.isRunning,
                                    onClick = {
                                        viewModel.startServer()
                                    }) {
                                    Icon(
                                        Icons.Default.SettingsBluetooth,
                                        contentDescription = "start server"
                                    )
                                }
                                IconButton(
                                    enabled = uiState.scanState != ScanState.Scanning,
                                    onClick = {
                                        viewModel.connect()
                                    }) {
                                    Icon(Icons.Default.BluetoothConnected, "connect to device")
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.messages) { message ->
                                    when (message) {
                                        is MyMessage -> {
                                            MyMessageView(message.message)
                                        }

                                        is RemoteMessage -> {
                                            Message(message.message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyMessageView(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Text(
            message,
            modifier = Modifier
                .myMessageBackground()
                .align(Alignment.CenterEnd),
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
fun Modifier.myMessageBackground(): Modifier {
    return this
        .background(
            color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 0.dp
            )
        )
        .padding(horizontal = 16.dp, vertical = 8.dp)
}

@Composable
fun Message(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        Text(
            message,
            modifier = Modifier
                .messageBackground()
                .align(Alignment.CenterStart),
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
}

@Composable
fun Modifier.messageBackground(): Modifier {
    return this
        .background(
            color = MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 0.dp,
                bottomEnd = 16.dp
            )
        )
        .padding(horizontal = 16.dp, vertical = 8.dp)
}

@Preview(widthDp = 200)
@Composable
private fun PreviewMessage() {
    EmobTheme {
        Surface {
            Column {
                MyMessage("Hello")
                Message("Hello!")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlePermissionsBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    onGranted: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(false) }
    val permissionState = rememberMultiplePermissionsState(permissions = BlePermissions) {
        isGranted = it.values.all { isGranted -> isGranted }
    }
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = if (isGranted) {
            contentAlignment
        } else {
            Alignment.Center
        }
    ) {
        if (isGranted) {
            onGranted()
        } else {
            Text("BLE permissions are not granted")
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Open app settings")
            }
        }
    }
}
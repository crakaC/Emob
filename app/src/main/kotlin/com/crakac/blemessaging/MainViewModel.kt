package com.crakac.blemessaging

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crakac.blemessaging.ble.EmobDevice
import com.crakac.blemessaging.ble.EmobScanner
import com.crakac.blemessaging.ble.EmobServer
import com.crakac.blemessaging.ble.ScanState
import com.crakac.blemessaging.di.CoroutineDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val emobServer: EmobServer,
    private val emobScanner: EmobScanner
) : ViewModel() {
    data class UiState(
        val connectionState: ConnectionState = ConnectionState.Idle,
        val scanState: ScanState = ScanState.Idle,
        val serverState: ServerState = ServerState.Idle
    )

    private val connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Idle)

    val uiState = combine(
        emobScanner.scanState,
        connectionState,
        emobServer.serverStateFlow
    ) { scanState, connectionState, server ->
        val serverState = if (server.advertising) {
            ServerState.Running(server.connectedDevices)
        } else {
            ServerState.Idle
        }
        UiState(connectionState, scanState, serverState)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data class Failed(val device: EmobDevice, val cause: Throwable?) : ConnectionState
        data class Connecting(val device: EmobDevice) : ConnectionState
        data class Connected(val device: EmobDevice) : ConnectionState
        data class Disconnected(val device: EmobDevice) : ConnectionState
    }

    sealed interface ServerState {
        data object Idle : ServerState
        data class Running(val connectedDevices: List<EmobDevice> = emptyList()) : ServerState
    }

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun startServer() {
        emobServer.start()
    }

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, BLUETOOTH_SCAN])
    fun connect() {
        viewModelScope.launch {
            emobScanner.scan(3.seconds)
                .distinctUntilChanged()
                .collect { device ->
                    try {
                        connectionState.update { ConnectionState.Connecting(device) }
                        emobScanner.connect(device)
                        connectionState.update { ConnectionState.Connected(device) }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to connect ${device.name}, ${device.address}")
                        connectionState.update { ConnectionState.Failed(device, e) }
                    }
                }
        }
    }

    override fun onCleared() {
        // TODO: アプリにBLEパーミッションを許可しない状態で到達するとクラッシュするので直す
        @SuppressLint("MissingPermission")
        emobServer.close()
    }
}
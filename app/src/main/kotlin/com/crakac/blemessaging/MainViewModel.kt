package com.crakac.blemessaging

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crakac.blemessaging.ble.EmobClient
import com.crakac.blemessaging.ble.EmobClientState
import com.crakac.blemessaging.ble.EmobScanner
import com.crakac.blemessaging.ble.EmobServer
import com.crakac.blemessaging.ble.EmobServerState
import com.crakac.blemessaging.ble.ScanState
import com.crakac.blemessaging.ble.model.Message
import com.crakac.blemessaging.ble.model.MyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class MainViewModel @Inject constructor(
    private val emobServer: EmobServer,
    private val emobScanner: EmobScanner,
    private val emobClient: EmobClient
) : ViewModel() {
    data class UiState(
        val clientState: EmobClientState = EmobClientState.Idle,
        val scanState: ScanState = ScanState.Idle,
        val serverState: EmobServerState = EmobServerState(),
        val messages: List<Message> = emptyList()
    )

    val messages: SharedFlow<Message> = emobServer.messagesFlow
    val myMessages = MutableSharedFlow<Message>()

    val messageQueue: Flow<List<Message>> =
        merge(myMessages, messages).runningFold(emptyList()) { acc, message ->
            acc + message
        }

    val uiState = combine(
        emobScanner.scanState,
        emobClient.clientState,
        emobServer.serverStateFlow,
        messageQueue
    ) { scanState, clientState, server, messageQueue ->
        UiState(clientState, scanState, server, messageQueue)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun startServer() {
        emobServer.open()
    }

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT, BLUETOOTH_SCAN])
    fun connect() {
        viewModelScope.launch {
            emobScanner.scan(3.seconds)
                .distinctUntilChanged()
                .collect { device ->
                    try {
                        emobClient.connect(device)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to connect ${device.name}, ${device.address}")
                    }
                }
        }
    }

    override fun onCleared() {
        emobServer.close()
        emobClient.disconnect()
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    fun sendMessage(message: String) {
        viewModelScope.launch {
            emobClient.sendMessage(message)
            myMessages.emit(MyMessage(message))
        }
    }
}
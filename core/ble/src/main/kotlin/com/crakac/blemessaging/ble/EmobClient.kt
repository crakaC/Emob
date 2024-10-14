package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.annotation.RequiresPermission
import com.crakac.blemessaging.ble.BleResource.EmobServiceUUID
import com.crakac.blemessaging.ble.di.BleScope
import com.crakac.blemessaging.ble.protocol.EmobFrame
import com.crakac.blemessaging.ble.protocol.EmobKeyExchange
import com.crakac.blemessaging.ble.protocol.EmobPlainText
import com.crakac.blemessaging.ble.protocol.parse
import com.crakac.blemessaging.ble.protocol.toByteArray
import com.crakac.blemessaging.ble.utils.enableNotification
import com.crakac.blemessaging.ble.utils.read
import com.crakac.blemessaging.ble.utils.write
import com.crakac.blemessaging.util.checkPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

class EmobClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @BleScope private val bleScope: CoroutineScope
) {
    private var clientJob: Job? = null
    private lateinit var gattClient: GattClient

    private val mutableClientState = MutableStateFlow<EmobClientState>(EmobClientState.Idle)
    val clientState = mutableClientState.asStateFlow()

    fun connect(device: EmobDevice) {
        if (!context.checkPermission(BLUETOOTH_CONNECT)) {
            Timber.d("No BLUETOOTH_CONNECT ")
            return
        }
        Timber.d("Connecting to ${device.address}, ${device.name}")
        clientJob = callbackFlow {
            gattClient = GattClient(context, device, this)
            awaitClose {
                gattClient.close()
            }
        }.catch {
            emit(GattState.Error(it))
        }.onEach {
            when (it) {

                is GattState.Connected -> {
                    // GATTで接続完了していてもServiceが見つかるまではデータの送受信は行えないため
                    // Connectingとする
                    mutableClientState.update { EmobClientState.Connecting(device) }
                }

                is GattState.ServiceDiscovered -> {
                    gattClient.enableNotification()
                    gattClient.keyExchange()
                    mutableClientState.update { EmobClientState.Connected(device) }
                }

                is GattState.Disconnected -> {
                    clientJob?.cancel()
                    mutableClientState.update { EmobClientState.Disconnected(device) }
                }

                is GattState.Error -> {
                    val error = it
                    mutableClientState.update { EmobClientState.Error(device, error.cause) }
                }
            }
        }.launchIn(bleScope)
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    suspend fun sendMessage(message: String) {
        val req = EmobPlainText(message)
        gattClient.write(req)
    }

    fun disconnect() {
        clientJob?.cancel()
        clientJob = null
    }

    @Suppress("MissingPermission")
    private class GattClient(
        context: Context,
        device: EmobDevice,
        private val channel: SendChannel<GattState>,
    ) : BluetoothGattCallback() {
        val gatt: BluetoothGatt = device.bluetoothDevice.connectGatt(context, false, this)

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("GATT connected")
                gatt.discoverServices()
            } else {
                channel.trySend(GattState.Disconnected)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt.getService(EmobServiceUUID) != null) {
                channel.trySend(GattState.ServiceDiscovered)
            } else {
                channel.trySend(GattState.Error(EmobServiceNotFoundException(status)))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val emobFrame = EmobFrame.parse(value)
            Timber.v("$emobFrame")
            when (emobFrame) {
                is EmobKeyExchange -> {
                    Timber.d("KeyExchange: received=${String(emobFrame.key)}")
                }

                else -> TODO()
            }
        }

        fun close() {
            gatt.disconnect()
            gatt.close()
        }

        suspend fun write(data: EmobFrame) {
            gatt.write(data.toByteArray())
        }

        suspend fun read() {
            val res = gatt.read()
            if (!res) {
                Timber.w("read failed")
            }
        }

        suspend fun keyExchange() {
            val req = EmobKeyExchange("Hello".toByteArray())
            write(req)
        }

        suspend fun enableNotification() {
            gatt.enableNotification()
        }
    }
}

private sealed interface GattState {
    class Error(val cause: Throwable) : GattState
    data object Connected : GattState
    data object ServiceDiscovered : GattState
    data object Disconnected : GattState
}

sealed interface EmobClientState {
    data object Idle : EmobClientState
    data class Error(val device: EmobDevice, val cause: Throwable?) : EmobClientState
    data class Connecting(val device: EmobDevice) : EmobClientState
    data class Connected(val device: EmobDevice) : EmobClientState
    data class Disconnected(val device: EmobDevice) : EmobClientState
}
package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import com.crakac.blemessaging.ble.BleResource.EmobService
import com.crakac.blemessaging.ble.BleResource.EmobServiceUUID
import com.crakac.blemessaging.ble.di.BleScope
import com.crakac.blemessaging.ble.model.Message
import com.crakac.blemessaging.ble.model.RemoteMessage
import com.crakac.blemessaging.ble.protocol.EmobFrame
import com.crakac.blemessaging.ble.protocol.EmobKeyExchange
import com.crakac.blemessaging.ble.protocol.EmobPlainText
import com.crakac.blemessaging.ble.protocol.parse
import com.crakac.blemessaging.ble.protocol.toByteArray
import com.crakac.blemessaging.ble.utils.notify
import com.crakac.blemessaging.ble.utils.toUpperHexString
import com.crakac.blemessaging.util.checkPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

private const val TAG = "EmobServer"

class EmobServer @Inject constructor(
    @ApplicationContext private val context: Context,
    @BleScope val bleScope: CoroutineScope
) {
    private val mutableServerState = MutableStateFlow(EmobServerState())
    val serverStateFlow = mutableServerState.asStateFlow()

    private val serverState: EmobServerState
        get() = serverStateFlow.value

    private val mutableMessagesFlow = MutableSharedFlow<Message>()
    val messagesFlow = mutableMessagesFlow.asSharedFlow()

    private val bluetoothManager = context.getSystemService<BluetoothManager>()!!

    private lateinit var gattServer: BluetoothGattServer

    private val bleAdvertiser: BluetoothLeAdvertiser =
        bluetoothManager.adapter.bluetoothLeAdvertiser

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Timber.tag(TAG).d("Advertise start success")
            mutableServerState.update { it.copy(advertising = true) }
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.tag(TAG).w("Advertise start failed, errorCode: $errorCode")
            mutableServerState.update { it.copy(advertising = false) }
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            val emobDevice = EmobDevice(device)
            Timber.tag(TAG)
                .d("Connection state is changed, status: $status, newState: $newState, device: $emobDevice")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mutableServerState.update { it + emobDevice }
            } else {
                mutableServerState.update { it - emobDevice }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Timber.tag(TAG).d(
                "onCharacteristicWriteRequest() requestId: $requestId, preparedWrite: $preparedWrite, " +
                        "responseNeeded: $responseNeeded, offset: $offset, value: ${value?.toUpperHexString()}"
            )
            if (value == null) {
                Timber.tag(TAG).w("onCharacteristicWriteRequest() value is null")
                return
            }
            val emobFrame = EmobFrame.parse(value)

            Timber.tag(TAG).d("Received EmobFrame: $emobFrame")
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, value)
            }
            when (emobFrame) {
                is EmobPlainText -> {
                    bleScope.launch {
                        mutableMessagesFlow.emit(
                            RemoteMessage(
                                EmobDevice(device),
                                emobFrame.text,
                                Instant.now()
                            )
                        )
                    }
                }

                is EmobKeyExchange -> {
                    // TODO 鍵交換を実装する

                    // 鍵交換が完了したことを通知する
                    gattServer.notify(device, EmobKeyExchange("Ack".toByteArray()).toByteArray())
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Timber.tag(TAG).d(
                "onDescriptorWriteRequest, device: $device, requestId: $requestId, preparedWrite: $preparedWrite, " +
                        "responseNeeded: $responseNeeded, offset: $offset, value: ${value?.toUpperHexString()}(${value?.decodeToString()})"
            )
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, null)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Timber.tag(TAG).d("onNotificationSent(), device: $device, status: $status")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Timber.tag(TAG).d("onMtuChanged(), device: $device, mtu: $mtu")
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG)
                .d("onPhyUpdate(), device: $device, txPhy: $txPhy, rxPhy: $rxPhy, status: $status")
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG)
                .d("onPhyRead(), device: $device, txPhy: $txPhy, rxPhy: $rxPhy, status: $status")
        }
    }

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun open() {
        if (serverState.isRunning) {
            Timber.i("Server is already running")
            return
        }
        mutableServerState.update { it.copy(isRunning = true) }
        gattServer = bluetoothManager.openGattServer(context, gattCallback)
        gattServer.addService(EmobService)
        startAdvertising()
    }

    @RequiresPermission(BLUETOOTH_ADVERTISE)
    private fun startAdvertising() {
        val setting = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(true)
            .addServiceUuid(EmobServiceUUID.parcel)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()

        bleAdvertiser.startAdvertising(setting, data, scanResponse, advertiseCallback)
    }

    fun close() {
        if (!::gattServer.isInitialized) return
        if (context.checkPermission(BLUETOOTH_ADVERTISE)) {
            bleAdvertiser.stopAdvertising(advertiseCallback)
        }
        if (context.checkPermission(BLUETOOTH_CONNECT)) {
            gattServer.close()
        }
    }
}

data class EmobServerState(
    val isRunning: Boolean = false,
    val advertising: Boolean = false,
    val connectedDevices: List<EmobDevice> = emptyList()
) {
    operator fun plus(emobDevice: EmobDevice): EmobServerState {
        return copy(connectedDevices = connectedDevices + emobDevice)
    }

    operator fun minus(emobDevice: EmobDevice): EmobServerState {
        return copy(connectedDevices = connectedDevices - emobDevice)
    }
}
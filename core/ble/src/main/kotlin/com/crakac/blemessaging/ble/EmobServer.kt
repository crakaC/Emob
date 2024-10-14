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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "EmobServer"

@OptIn(ExperimentalStdlibApi::class)
class EmobServer @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val connectedDevices: MutableSet<BluetoothDevice> = LinkedHashSet()

    // TODO: AdvertiseしていなくてもServerがRunningな状態を表現できるようにする
    private val _serverStateFlow = MutableStateFlow(
        EmobServerState(
            advertising = false,
            connectedDevices = emptyList()
        )
    )
    val serverStateFlow: StateFlow<EmobServerState> = _serverStateFlow.asStateFlow()
    private var serverState: EmobServerState
        get() = _serverStateFlow.value
        set(value) {
            _serverStateFlow.value = value
        }

    private val bluetoothManager = context.getSystemService<BluetoothManager>()!!

    private lateinit var gattServer: BluetoothGattServer

    private val bleAdvertiser: BluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Timber.tag(TAG).d("Advertise start success")
            _serverStateFlow.update { it.copy(advertising = true) }
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.w("Advertise start failed, errorCode: $errorCode")
            _serverStateFlow.update { it.copy(advertising = false) }
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Timber.tag(TAG)
                .d("Connection state is changed, status: $status, newState: $newState, device: ${device.toEmobDevice()}")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevices.add(device)
                _serverStateFlow.update { it.copy(connectedDevices = connectedDevices.map { it.toEmobDevice() }) }
            } else {
                connectedDevices.remove(device)
                _serverStateFlow.update { it.copy(connectedDevices = connectedDevices.map { it.toEmobDevice() }) }
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
                "onCharacteristicWriteRequest() requestId: $requestId, characteristic: ${characteristic.uuid}, preparedWrite: $preparedWrite, " +
                        "responseNeeded: $responseNeeded, offset: $offset, value: ${value?.toHexString()}(${value?.decodeToString()})"
            )
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, null)
            }
            gattServer.notify(device, "World!".toByteArray())
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
                        "responseNeeded: $responseNeeded, offset: $offset, value: ${value?.toHexString()}(${value?.decodeToString()})"
            )
            gattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, null)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Timber.tag(TAG).d("onNotificationSent(), device: $device, status: $status")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Timber.tag(TAG).d("onMtuChanged(), device: $device, mtu: $mtu")
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG).d("onPhyUpdate(), device: $device, txPhy: $txPhy, rxPhy: $rxPhy, status: $status")
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Timber.tag(TAG).d("onPhyRead(), device: $device, txPhy: $txPhy, rxPhy: $rxPhy, status: $status")
        }
    }

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun start() {
        if (serverState.advertising) {
            Timber.i("Server is already running")
            return
        }
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

    @RequiresPermission(allOf = [BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT])
    fun close() {
        if (::gattServer.isInitialized) {
            bleAdvertiser.stopAdvertising(advertiseCallback)
            gattServer.close()
        }
    }
}

data class EmobServerState(
    val advertising: Boolean,
    val connectedDevices: List<EmobDevice>
)
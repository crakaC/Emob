package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.CheckResult
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import com.crakac.blemessaging.ble.BleResource.EmobServiceUUID
import com.crakac.blemessaging.ble.di.BleScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

private const val TAG = "EmobScanner"

@OptIn(ExperimentalStdlibApi::class)
class EmobScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @BleScope private val bleScope: CoroutineScope
) {
    private val bluetoothManager = context.getSystemService<BluetoothManager>()!!
    private val bleScanner: BluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

    private val mutableScanState: MutableStateFlow<ScanState> = MutableStateFlow(ScanState.Idle)
    val scanState: StateFlow<ScanState> = mutableScanState.asStateFlow()

    @RequiresPermission(allOf = [BLUETOOTH_SCAN, BLUETOOTH_CONNECT])
    @CheckResult
    fun scan(duration: Duration): Flow<EmobDevice> = callbackFlow {
        mutableScanState.value = ScanState.Scanning
        val scanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.emobDevice
                Timber.tag(TAG).d("Scan result: $device")
                trySend(device)
            }

            override fun onScanFailed(errorCode: Int) {
                close(EmobScannerException(errorCode))
            }
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(EmobServiceUUID.parcel)
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        bleScanner.startScan(listOf(filter), settings, scanCallback)
        timeout(duration)
        awaitClose {
            bleScanner.stopScan(scanCallback)
            mutableScanState.update { ScanState.Idle }
        }
    }.catch { t ->
        if (t is CancellationException) throw t
        if (t is EmobScannerException) {
            mutableScanState.update { ScanState.Failed(t.errorCode) }
        }
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    suspend fun connect(device: EmobDevice) {
        Timber.tag(TAG).d("Connecting to ${device.address}, ${device.name}")
        val gatt = suspendCancellableCoroutine { continuation ->
            device.bluetoothDevice.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    if (continuation.isActive) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Timber.tag(TAG).d("GATT connected")
                            @SuppressLint("MissingPermission")
                            gatt.discoverServices()
                        } else {
                            continuation.resumeWithException(EmobConnectionException(status))
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Timber.tag(TAG).d("EmobService is found")
                        continuation.resume(gatt) { cause, _, _ -> }
                    } else {
                        continuation.resumeWithException(EmobServiceNotFoundException(status))
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    Timber.d("onCharacteristicChanged(): value=${value.toHexString()}(${value.decodeToString()})")
                }
            })
        }
        gatt.enableNotification()
        gatt.write("Hello!".toByteArray())
    }
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    class Failed(val errorCode: Int) : ScanState {
        override fun toString(): String {
            return "ScanState.Failed(status=$errorCode)"
        }
    }
}

sealed class EmobException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
class EmobScannerException(val errorCode: Int) : EmobException("Scan failed. errorCode = $errorCode")
class EmobConnectionException(val status: Int) : EmobException("Connection failed. status = $status")
class EmobServiceNotFoundException(val status: Int): EmobException("EmobService is not found. status = $status")
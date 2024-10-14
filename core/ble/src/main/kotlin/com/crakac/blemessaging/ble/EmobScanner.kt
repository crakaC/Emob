package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
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
import com.crakac.blemessaging.ble.utils.timeout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration

private const val TAG = "EmobScanner"

class EmobScanner @Inject constructor(
    @ApplicationContext private val context: Context,
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
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.emobDevice
                Timber.tag(TAG).d("Scan result: $device")
                trySend(device)
            }

            override fun onScanFailed(errorCode: Int) {
                close(BleScanFailedException(errorCode))
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
        if (t is TimeoutCancellationException) {
            mutableScanState.update { ScanState.Failed(EmobDeviceNotFoundException(t)) }
        }
        if (t is BleScanFailedException) {
            mutableScanState.update { ScanState.Failed(EmobScannerException(t.statusCode)) }
        }
    }
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Failed(val error: EmobException) : ScanState
}

sealed class EmobException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

class EmobScannerException(val errorCode: Int) :
    EmobException("Scan failed. errorCode = $errorCode")

class EmobDeviceNotFoundException(cause: Throwable? = null) :
    EmobException("Scan failed. Device not found", cause)

class EmobConnectionException(val status: Int) :
    EmobException("Connection failed. status = $status")

class EmobServiceNotFoundException(val status: Int) :
    EmobException("EmobService is not found. status = $status")
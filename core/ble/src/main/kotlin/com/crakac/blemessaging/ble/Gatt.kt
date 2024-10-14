package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.crakac.blemessaging.ble.BleResource.EmobCharacteristic
import com.crakac.blemessaging.ble.BleResource.EmobCharacteristicUUID
import com.crakac.blemessaging.ble.BleResource.EmobDescriptorUUID
import com.crakac.blemessaging.ble.BleResource.EmobServiceUUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@RequiresPermission(BLUETOOTH_CONNECT)
@Suppress("DEPRECATION")
internal suspend fun BluetoothGatt.write(
    value: ByteArray,
    timeout: Duration = 10.seconds,
) = withTimeout(timeout) {
    val characteristic = emobCharacteristic
    val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        retryUntilBusy {
            writeCharacteristic(characteristic, value, writeType) == BluetoothStatusCodes.SUCCESS
        }
    } else {
        characteristic.writeType = writeType
        characteristic.value = value
        writeCharacteristic(characteristic)
    }
}

@SuppressLint("MissingPermission")
internal fun BluetoothGatt.read() {
    val characteristic = emobCharacteristic
    readCharacteristic(characteristic)
}

internal val BluetoothGatt.emobCharacteristic: BluetoothGattCharacteristic
    get() {
        val service = getService(EmobServiceUUID) ?: error("EmobService not found")
        return service.getCharacteristic(EmobCharacteristicUUID) ?: error("Characteristic not found")
    }

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
internal suspend fun BluetoothGatt.enableNotification(): Boolean {
    val characteristic = emobCharacteristic
    setCharacteristicNotification(characteristic, true)
    val descriptor = characteristic.getDescriptor(EmobDescriptorUUID) ?: error("Descriptor not found")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        retryUntilBusy {
            writeDescriptor(
                descriptor,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            ) == BluetoothStatusCodes.SUCCESS
        }
    } else {
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        writeDescriptor(descriptor)
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
internal suspend fun BluetoothGattServer.notify(device: BluetoothDevice, value: ByteArray): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        retryUntilBusy {
            notifyCharacteristicChanged(device, EmobCharacteristic, false, value) == BluetoothStatusCodes.SUCCESS
        }
    } else {
        val characteristic = EmobCharacteristic.apply {
            setValue(value)
        }
        notifyCharacteristicChanged(device, characteristic, false)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun <T> retryUntilBusy(block: () -> T): T {
    var result: T
    while (block().also { result = it } == ERROR_GATT_WRITE_REQUEST_BUSY) {
        Timber.d("waiting for gatt")
        delay(100.milliseconds)
    }
    return result
}
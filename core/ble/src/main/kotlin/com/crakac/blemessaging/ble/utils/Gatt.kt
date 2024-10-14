package com.crakac.blemessaging.ble.utils

import android.Manifest.permission.BLUETOOTH_CONNECT
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
internal suspend fun BluetoothGatt.write(
    value: ByteArray,
    timeout: Duration = 10.seconds,
) = withTimeout(timeout) {
    val characteristic = emobCharacteristic
    val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        retryUntilBusy {
            writeCharacteristic(characteristic, value, writeType)
        }
    } else {
        characteristic.writeType = writeType
        @Suppress("DEPRECATION")
        characteristic.value = value
        @Suppress("DEPRECATION")
        writeCharacteristic(characteristic)
    }
    if (status != BluetoothStatusCodes.SUCCESS) {
        Timber.e("write failed: $status")
    }
}

@RequiresPermission(BLUETOOTH_CONNECT)
internal fun BluetoothGatt.read(): Boolean {
    val characteristic = emobCharacteristic
    return readCharacteristic(characteristic)
}

internal val BluetoothGatt.emobCharacteristic: BluetoothGattCharacteristic
    get() {
        val service = getService(EmobServiceUUID) ?: error("EmobService not found")
        return service.getCharacteristic(EmobCharacteristicUUID)
            ?: error("Characteristic not found")
    }

@RequiresPermission(BLUETOOTH_CONNECT)
internal suspend fun BluetoothGatt.enableNotification(): Boolean {
    val characteristic = emobCharacteristic
    setCharacteristicNotification(characteristic, true)
    val descriptor =
        characteristic.getDescriptor(EmobDescriptorUUID) ?: error("Descriptor not found")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        retryUntilBusy {
            writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } == BluetoothStatusCodes.SUCCESS
    } else {
        @Suppress("DEPRECATION")
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        @Suppress("DEPRECATION")
        writeDescriptor(descriptor)
    }
}

internal val BluetoothGattServer.emobCharacteristic: BluetoothGattCharacteristic
    get() {
        val service = getService(EmobServiceUUID) ?: error("EmobService not found")
        return service.getCharacteristic(EmobCharacteristicUUID)
            ?: error("Characteristic not found")
    }

@RequiresPermission(BLUETOOTH_CONNECT)
internal fun BluetoothGattServer.notify(device: BluetoothDevice, value: ByteArray): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notifyCharacteristicChanged(
            device,
            emobCharacteristic,
            false,
            value
        ) == BluetoothStatusCodes.SUCCESS
    } else {
        val characteristic = emobCharacteristic
        @Suppress("DEPRECATION")
        characteristic.setValue(value)
        @Suppress("DEPRECATION")
        notifyCharacteristicChanged(device, characteristic, false)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private suspend fun retryUntilBusy(block: () -> Int): Int {
    var result: Int
    while (block().also { result = it } == ERROR_GATT_WRITE_REQUEST_BUSY) {
        Timber.d("waiting for gatt")
        delay(100.milliseconds)
    }
    return result
}
package com.crakac.blemessaging.ble

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import android.os.ParcelUuid
import java.util.UUID

internal object BleResource {
    val EmobServiceUUID: UUID = UUID.fromString("eb5ac374-b364-4b90-bf05-0000000000")
    val EmobCharacteristicUUID: UUID = UUID.fromString("eb5ac374-b364-4b90-bf05-0000000001")
    val EmobDescriptorUUID: UUID = UUID.fromString("eb5ac374-b364-4b90-bf05-0000000002")

    private val EmobCharacteristic: BluetoothGattCharacteristic
        get() = BluetoothGattCharacteristic(
            EmobCharacteristicUUID,
            PROPERTY_READ or PROPERTY_WRITE or PROPERTY_NOTIFY,
            PERMISSION_READ or PERMISSION_WRITE
        ).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    EmobDescriptorUUID,
                    PERMISSION_READ or PERMISSION_WRITE
                )
            )
        }
    val EmobService = BluetoothGattService(EmobServiceUUID, SERVICE_TYPE_PRIMARY).apply {
        addCharacteristic(EmobCharacteristic)
    }
}

internal val UUID.parcel: ParcelUuid get() = ParcelUuid(this)

val BlePermissions = listOf(
    BLUETOOTH_CONNECT,
    BLUETOOTH_SCAN,
    BLUETOOTH_ADVERTISE,
)

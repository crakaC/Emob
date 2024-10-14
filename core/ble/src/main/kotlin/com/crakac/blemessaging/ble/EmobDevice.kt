package com.crakac.blemessaging.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import com.crakac.blemessaging.ble.model.Device

data class EmobDevice(
    override val name: String,
    val address: String,
    val bluetoothDevice: BluetoothDevice
) : Device {
    @SuppressLint("MissingPermission")
    constructor(device: BluetoothDevice) : this(
        name = device.name ?: "Unknown",
        address = device.address,
        bluetoothDevice = device
    )

    override fun toString(): String {
        return "$name ($address)"
    }
}

internal val ScanResult.emobDevice: EmobDevice get() = EmobDevice(device)
package com.crakac.blemessaging.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

data class EmobDevice(
    val name: String,
    val address: String,
    val bluetoothDevice: BluetoothDevice
) {
    override fun toString(): String {
        return "$name ($address)"
    }
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.toEmobDevice(): EmobDevice {
    return EmobDevice(
        name = this.name ?: "Unknown",
        address = this.address,
        bluetoothDevice = this
    )
}

val ScanResult.emobDevice: EmobDevice get() = device.toEmobDevice()
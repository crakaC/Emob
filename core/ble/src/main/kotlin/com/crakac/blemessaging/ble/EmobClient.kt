package com.crakac.blemessaging.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

class EmobClient : BluetoothGattCallback() {
    override fun onPhyUpdate(
        gatt: BluetoothGatt,
        txPhy: Int,
        rxPhy: Int,
        status: Int
    ) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status)
    }

    override fun onPhyRead(
        gatt: BluetoothGatt,
        txPhy: Int,
        rxPhy: Int,
        status: Int
    ) {
        super.onPhyRead(gatt, txPhy, rxPhy, status)
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        super.onConnectionStateChange(gatt, status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ) {
        super.onDescriptorRead(gatt, descriptor, status, value)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
    }

    override fun onReliableWriteCompleted(
        gatt: BluetoothGatt,
        status: Int
    ) {
        super.onReliableWriteCompleted(gatt, status)
    }

    override fun onReadRemoteRssi(
        gatt: BluetoothGatt,
        rssi: Int,
        status: Int
    ) {
        super.onReadRemoteRssi(gatt, rssi, status)
    }

    override fun onMtuChanged(
        gatt: BluetoothGatt,
        mtu: Int,
        status: Int
    ) {
        super.onMtuChanged(gatt, mtu, status)
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        super.onServiceChanged(gatt)
    }
}
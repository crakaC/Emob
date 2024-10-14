package com.crakac.blemessaging.ble.protocol

import com.crakac.blemessaging.ble.utils.toUpperHexString

internal data class EmobKeyExchange(
    val key: ByteArray,
) : EmobFrame {
    override val header: EmobHeader = EmobHeader.KeyExchange
    override val payload: ByteArray = key

    override fun toString(): String {
        return "EmobKeyExchange(key=${key.toUpperHexString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmobKeyExchange

        if (!key.contentEquals(other.key)) return false
        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + header.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
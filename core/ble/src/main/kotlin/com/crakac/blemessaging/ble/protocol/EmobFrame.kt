package com.crakac.blemessaging.ble.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal sealed interface EmobFrame {
    val header: EmobHeader
    val payload: ByteArray
    val size: Int get() = header.size + payload.size

    companion object
}

internal fun EmobFrame.toByteArray(): ByteArray {
    return ByteBuffer.allocate(size)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(header.value)
        .put(payload)
        .array()
}

internal fun EmobFrame.Companion.parse(data: ByteArray): EmobFrame {
    val buf = ByteBuffer.wrap(data)
        .order(ByteOrder.LITTLE_ENDIAN)
    val header = EmobHeader(buf.get())
    val payload = buf.array().copyOfRange(buf.position(), buf.limit())
    return when (header) {
        EmobHeader.PlainText -> EmobPlainText(payload)
        EmobHeader.KeyExchange -> EmobKeyExchange(payload)
        EmobHeader.EncryptedText -> TODO("not implemented")
        else -> error("Unknown Header")
    }
}
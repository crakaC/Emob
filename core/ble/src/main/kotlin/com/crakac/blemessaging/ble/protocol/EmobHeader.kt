package com.crakac.blemessaging.ble.protocol

@JvmInline
internal value class EmobHeader(val value: Byte) {
    companion object {
        val PlainText = EmobHeader(0x00)
        val KeyExchange = EmobHeader(0x01)
        val EncryptedText = EmobHeader(0x02)
    }

    val size: Int get() = Byte.SIZE_BYTES
}
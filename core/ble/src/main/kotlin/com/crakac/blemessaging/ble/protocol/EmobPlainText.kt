package com.crakac.blemessaging.ble.protocol

internal data class EmobPlainText(
    val text: String
) : EmobFrame {
    override val header: EmobHeader = EmobHeader.PlainText
    override val payload: ByteArray = text.toByteArray()

    constructor(payload: ByteArray) : this(String(payload))
}
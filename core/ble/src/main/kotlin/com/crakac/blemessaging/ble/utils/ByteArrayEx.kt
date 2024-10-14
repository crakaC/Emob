package com.crakac.blemessaging.ble.utils

@OptIn(ExperimentalStdlibApi::class)
internal fun ByteArray.toUpperHexString(): String = toHexString(HexFormat.UpperCase)

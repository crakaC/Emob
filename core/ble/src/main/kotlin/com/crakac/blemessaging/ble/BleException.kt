package com.crakac.blemessaging.ble

interface BleException {
    val message: String?
    val cause: Throwable?
}

class BleTimeoutException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause), BleException

class BleScanFailedException(
    val statusCode: Int,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause), BleException
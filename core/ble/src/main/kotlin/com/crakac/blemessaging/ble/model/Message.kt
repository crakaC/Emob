package com.crakac.blemessaging.ble.model

import java.time.Instant

interface Device {
    val name: String
}

class MyDevice : Device {
    override val name: String = "My Device"
}

sealed interface Message {
    val device: Device
    val message: String
    val timestamp: Instant
}

data class RemoteMessage(
    override val device: Device,
    override val message: String,
    override val timestamp: Instant
) : Message

data class MyMessage(
    override val device: MyDevice,
    override val message: String,
    override val timestamp: Instant
) : Message {
    constructor(message: String) : this(
        device = MyDevice(),
        message = message,
        timestamp = Instant.now()
    )
}
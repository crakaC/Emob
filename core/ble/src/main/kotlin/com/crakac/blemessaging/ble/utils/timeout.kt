package com.crakac.blemessaging.ble.utils

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

internal fun <T> ProducerScope<T>.timeout(duration: Duration) {
    launch {
        try {
            withTimeout(duration) {
                awaitCancellation()
            }
        } catch (e: TimeoutCancellationException) {
            close(e)
        }
    }
}
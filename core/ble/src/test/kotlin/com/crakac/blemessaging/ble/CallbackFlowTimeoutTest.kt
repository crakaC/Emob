package com.crakac.blemessaging.ble

import app.cash.turbine.test
import com.crakac.blemessaging.ble.utils.timeout
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CallbackFlowTimeoutTest {
    @Test
    fun timeout() = runTest {
        val flow = callbackFlow<Unit> {
            timeout(1.seconds)
            awaitClose {
                println("closed")
            }
        }
        flow.test {
            val error = awaitError()
            error should beInstanceOf<TimeoutCancellationException>()
        }
    }

    @Test
    fun collect() = runTest {
        val flow = callbackFlow<Int> {
            timeout(1.seconds)
            send(1)
            close()
            awaitClose {
                println("closed")
            }
        }
        flow.test {
            awaitItem() shouldBe 1
            awaitComplete()
        }
    }

    @Test
    fun cancel(): Unit = runBlocking {
        var msg = ""
        val job = launch {
            val flow = callbackFlow<Unit> {
                timeout(1.seconds)
                send(Unit)
                awaitClose {
                    msg = "closed"
                    println("closed")
                }
            }
            flow.collect {
                cancel()
            }
        }
        job.join()
        msg shouldBe "closed"
    }
}
package com.crakac.blemessaging.di

import kotlin.coroutines.CoroutineContext

interface CoroutineDispatchers {
    val main: CoroutineContext
    val io: CoroutineContext
    val compute: CoroutineContext
}
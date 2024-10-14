package com.crakac.blemessaging.di

import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class AppCoroutineDispatchers @Inject constructor() : CoroutineDispatchers {
    override val main: CoroutineContext = Dispatchers.Main
    override val io: CoroutineContext = Dispatchers.IO
    override val compute: CoroutineContext = Dispatchers.Default
}
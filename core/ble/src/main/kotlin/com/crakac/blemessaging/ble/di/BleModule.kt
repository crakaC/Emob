package com.crakac.blemessaging.ble.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@InstallIn(SingletonComponent::class)
@Module
internal object BleModule {
    @Provides
    @BleScope
    fun provideBleScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BleDevice"))
    }
}

@Retention(AnnotationRetention.SOURCE)
internal annotation class BleScope
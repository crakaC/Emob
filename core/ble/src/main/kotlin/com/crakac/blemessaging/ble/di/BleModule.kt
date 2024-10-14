package com.crakac.blemessaging.ble.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object BleModule {
    @Provides
    @Singleton
    @BleScope
    fun provideBleScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BleDevice"))
    }
}

@Retention(AnnotationRetention.SOURCE)
annotation class BleScope
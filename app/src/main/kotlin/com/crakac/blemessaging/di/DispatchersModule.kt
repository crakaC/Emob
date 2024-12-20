package com.crakac.blemessaging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DispatchersModule {
    @Binds
    fun bindAppCoroutineDispatchers(dispatchers: AppCoroutineDispatchers): CoroutineDispatchers
}
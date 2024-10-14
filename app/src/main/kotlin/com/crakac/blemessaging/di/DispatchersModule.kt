package com.crakac.blemessaging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DispatchersModule {
    @Binds
    @Singleton
    fun bindAppCoroutineDispatchers(dispatchers: AppCoroutineDispatchers): CoroutineDispatchers
}
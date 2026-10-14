package com.example.appeventtracker.di.AppDatabase

import com.example.appeventtracker.delegates.EventIngestionDelegate
import com.example.appeventtracker.worker.MockEventIngestionDelegate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DelegatesModule {

    @Binds
    @Singleton
    abstract fun bindEventIngestionDelegate(
        implementation: MockEventIngestionDelegate
    ): EventIngestionDelegate

}
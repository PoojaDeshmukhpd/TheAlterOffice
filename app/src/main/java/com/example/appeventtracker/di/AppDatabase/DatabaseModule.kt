package com.example.appeventtracker.di.AppDatabase

import android.content.Context
import androidx.room.Room
import com.example.appeventtracker.data.local.EventDao
import com.example.appeventtracker.data.local.EventDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEventDatabase(
        @ApplicationContext context: Context
    ): EventDatabase {
        return Room.databaseBuilder(
            context,
            EventDatabase::class.java,
            "event_tracker.db"
        ).build()
    }

    @Provides
    fun provideEventDao(
        database: EventDatabase
    ): EventDao {
        return database.eventDao()
    }
}

package com.example.appeventtracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EventTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            android.util.Log.d(
                "AppDebug",
                "Building WorkManager configuration with HiltWorkerFactory"
            )

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }
}

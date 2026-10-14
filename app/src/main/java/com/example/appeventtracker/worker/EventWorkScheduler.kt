package com.example.appeventtracker.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventWorkScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleProcessing() {
        Log.d("EventScheduler", "scheduleProcessing() called")

        val request = OneTimeWorkRequestBuilder<EventWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        Log.d("EventScheduler", "Enqueuing worker. workId=${request.id}")

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleRetry(delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<EventWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // Use a separate delayed request so the currently running
        // queue worker does not suppress its own retry schedule.
        workManager.enqueue(request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "event_queue_processing"

    }
}
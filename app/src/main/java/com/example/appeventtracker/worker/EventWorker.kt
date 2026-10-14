package com.example.appeventtracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appeventtracker.data.local.EventDao
import com.example.appeventtracker.delegates.EventIngestionDelegate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class EventWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventDao: EventDao,
    private val ingestionDelegate: EventIngestionDelegate,
    private val eventWorkScheduler: EventWorkScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started. workId=$id")

        return try {
            val now = System.currentTimeMillis()

            val recoveredCount = eventDao.recoverStaleEvents(
                staleBefore = now - STALE_EVENT_TIMEOUT_MS,
                updatedAt = now
            )

            Log.d(TAG, "Recovered stale events. rows=$recoveredCount")

            processQueue()

            Log.d(TAG, "Queue processing completed. workId=$id")
            Result.success()
        } catch (exception: CancellationException) {
            Log.d(TAG, "Worker cancelled. workId=$id")
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Worker failed; WorkManager will retry", exception)
            Result.retry()
        }
    }


    private suspend fun processQueue() {
        while (!isStopped) {
            val nextEvent = eventDao.getNextEvent(
                now = System.currentTimeMillis()
            )

            if (nextEvent == null) {
                Log.d(TAG, "No eligible events remaining")
                break
            }

            val claimed = eventDao.claimEvent(
                eventId = nextEvent.id,
                updatedAt = System.currentTimeMillis()
            )

            if (claimed != 1) {
                Log.d(TAG, "Event ${nextEvent.id} was claimed elsewhere")
                continue
            }

            val event = eventDao.getEventById(nextEvent.id) ?: continue

            Log.d(
                TAG,
                "Processing event: id=${event.id}, type=${event.eventType}, " +
                        "retryCount=${event.retryCount}"
            )

            try {
                ingestionDelegate.ingest(event)

                eventDao.updateEventStatus(
                    eventId = event.id,
                    status = "PROCESSED",
                    retryCount = event.retryCount,
                    lastError = null,
                    updatedAt = System.currentTimeMillis()
                )

                Log.d(
                    TAG,
                    "Event processed successfully: id=${event.id}, " +
                            "type=${event.eventType}"
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {

                val nextRetryCount = event.retryCount + 1
                val now = System.currentTimeMillis()

                if (nextRetryCount >= MAX_RETRIES) {

                    eventDao.updateEventStatus(
                        eventId = event.id,
                        status = "FAILED",
                        retryCount = nextRetryCount,
                        lastError = exception.message ?: "Ingestion failed",
                        updatedAt = now
                    )

                } else {

                    // 1. Mark the event as RETRYING
                    eventDao.markEventForRetry(
                        eventId = event.id,
                        retryCount = nextRetryCount,
                        nextAttemptAt = now + RETRY_DELAY_MS,
                        lastError = exception.message ?: "Ingestion failed",
                        updatedAt = now
                    )

                    // 2. Schedule a future retry
                    eventWorkScheduler.scheduleRetry(RETRY_DELAY_MS)

                    // Continue processing the next eligible event.
                }
            }
        }
    }


    private companion object {
        const val TAG = "EventWorker"
        const val STALE_EVENT_TIMEOUT_MS = 2 * 60 * 1000L
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 10_000L
    }
}
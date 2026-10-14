package com.example.appeventtracker.data

import android.util.Log
import com.example.appeventtracker.data.local.EnqueueResult
import com.example.appeventtracker.data.parser.EventJsonParser
import com.example.appeventtracker.data.session.SessionManager
import com.example.appeventtracker.repositories.EventRepository
import com.example.appeventtracker.worker.EventWorkScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventEnqueueCoordinator @Inject constructor(
    private val parser: EventJsonParser,
    private val sessionManager: SessionManager,
    private val repository: EventRepository,
    private val workScheduler: EventWorkScheduler
) {
    suspend fun enqueueJson(json: String): List<EnqueueResult> {
        Log.d(TAG, "enqueueJson() called")

        // Validate the entire JSON input before saving any event.
        val events = try {
            parser.parse(json).also {
                Log.d(TAG, "JSON parsed successfully. Event count=${it.size}")
            }
        } catch (exception: Exception) {
            Log.e(TAG, "JSON parsing failed", exception)
            throw exception
        }

        // One session ID is used for this submitted batch.
        val sessionId = sessionManager.getCurrentSessionId()
        Log.d(TAG, "Session ID obtained")

        val results = events.map { event ->
            repository.enqueueEvent(event, sessionId)
        }

        Log.d(TAG, "Enqueue completed. Results=${results.size}")

        val hasNewEvents = results.any { it is EnqueueResult.Enqueued }

        if (hasNewEvents) {
            Log.d(TAG, "New events inserted. Calling scheduleProcessing()")
            workScheduler.scheduleProcessing()
        } else {
            Log.d(TAG, "No new events inserted. Scheduling skipped")
        }

        return results
    }

    private companion object {
        const val TAG = "EventCoordinator"
    }
}
package com.example.appeventtracker.worker

import android.util.Log
import com.example.appeventtracker.data.local.EventEntity
import com.example.appeventtracker.delegates.EventIngestionDelegate
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockEventIngestionDelegate @Inject constructor() :
    EventIngestionDelegate {

    override suspend fun ingest(event: EventEntity) {
        Log.d(
            TAG,
            "Ingestion started: eventId=${event.id}, type=${event.eventType}"
        )

        val delayMillis = Random.nextLong(1_000, 5_001)
        Log.d(TAG, "Simulated ingestion delay: ${delayMillis}ms")
        delay(delayMillis)

        if (Random.nextInt(100) < 20) {
            Log.e(
                TAG,
                "Ingestion failed: eventId=${event.id}, type=${event.eventType}"
            )
            throw IngestionException(
                "Simulated ingestion failure for event ${event.id}"
            )
        }

        Log.d(
            TAG,
            "Ingestion succeeded: eventId=${event.id}, type=${event.eventType}"
        )
    }

    private companion object {
        const val TAG = "MockIngestion"
    }
}

class IngestionException(message: String) : Exception(message)
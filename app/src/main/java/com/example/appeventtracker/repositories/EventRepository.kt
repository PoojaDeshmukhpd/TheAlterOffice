package com.example.appeventtracker.repositories

import androidx.room.withTransaction
import com.example.appeventtracker.data.local.EnqueueResult
import com.example.appeventtracker.data.local.EventDao
import com.example.appeventtracker.data.local.EventDatabase
import com.example.appeventtracker.data.local.EventEntity
import com.example.appeventtracker.data.local.NewEvent
import com.example.appeventtracker.data.model.EventType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val database: EventDatabase,
    private val eventDao: EventDao
) {

    suspend fun enqueueEvent(
        newEvent: NewEvent,
        sessionId: String
    ): EnqueueResult {

        val deduplicationKey = when (newEvent.eventType.name) {
            "INSTALL" -> "INSTALL"
            "VISIT" -> "VISIT:$sessionId"
            "PURCHASE", "ADD_TO_CART" -> null
            else -> return EnqueueResult.Rejected(
                "Unsupported event type: ${newEvent.eventType}"
            )
        }

        val event = EventEntity(
            eventType = newEvent.eventType.name,
            payload = newEvent.payload,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            deduplicationKey = deduplicationKey
        )

        return database.withTransaction {
            val insertedId = eventDao.insertEvent(event)

            if (insertedId == -1L) {
                EnqueueResult.Duplicate
            } else {
                EnqueueResult.Enqueued(insertedId)
            }
        }
    }
}
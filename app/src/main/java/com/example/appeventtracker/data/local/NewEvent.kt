package com.example.appeventtracker.data.local

import com.example.appeventtracker.data.model.EventType


/**
 * Represents an event received from the input JSON.
 *
 * Timestamp and session ID are intentionally not supplied here:
 * the application assigns them when the event is enqueued.
 */
data class NewEvent(
    val eventType: EventType,
    val payload: String
)

sealed interface EnqueueResult {
    data class Enqueued(val eventId: Long) : EnqueueResult
    data object Duplicate : EnqueueResult
    data class Rejected(val reason: String) : EnqueueResult
}
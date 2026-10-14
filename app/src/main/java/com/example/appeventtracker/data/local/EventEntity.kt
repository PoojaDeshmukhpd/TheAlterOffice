package com.example.appeventtracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["deduplicationKey"], unique = true)
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // INSTALL or PURCHASE.
    val eventType: String,

    val payload: String,

    val timestamp: Long,

    val sessionId: String,

    val deduplicationKey: String?,

    val status: String = "PENDING",

    val retryCount: Int = 0,

    val lastError: String? = null,

    val updatedAt: Long = timestamp,
    val nextAttemptAt: Long? = null
)
package com.example.appeventtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EventEntity): Long

    // Observe all events that are waiting or being processed.
    @Query("""
        SELECT * FROM events
        WHERE status IN ('PENDING', 'IN_PROGRESS')
        ORDER BY timestamp ASC
    """)
    fun observePendingEvents(): Flow<List<EventEntity>>


    @Query("""
    SELECT * FROM events
    WHERE status IN ('PENDING', 'IN_PROGRESS', 'PROCESSED')
    ORDER BY timestamp DESC
""")
    fun observeQueueEvents(): Flow<List<EventEntity>>


    // Observe failed events and events waiting for a retry.
    @Query("""
    SELECT * FROM events
    WHERE status IN ('FAILED', 'RETRYING')
    ORDER BY updatedAt DESC
""")
    fun observeFailedAndRetryingEvents(): Flow<List<EventEntity>>


    // Fetch the next event eligible for processing.
    @Query("""
    SELECT * FROM events
    WHERE status = 'PENDING'
       OR (
           status = 'RETRYING'
           AND nextAttemptAt IS NOT NULL
           AND nextAttemptAt <= :now
       )
    ORDER BY timestamp ASC
    LIMIT 1
""")
    suspend fun getNextEvent(now: Long): EventEntity?

    @Query("""
    UPDATE events
    SET status = 'RETRYING',
        retryCount = :retryCount,
        nextAttemptAt = :nextAttemptAt,
        lastError = :lastError,
        updatedAt = :updatedAt
    WHERE id = :eventId
""")
    suspend fun markEventForRetry(
        eventId: Long,
        retryCount: Int,
        nextAttemptAt: Long,
        lastError: String?,
        updatedAt: Long
    )

    // Atomically claim an event so another worker cannot claim it too.
    @Query("""
        UPDATE events
        SET status = 'IN_PROGRESS',
            updatedAt = :updatedAt
        WHERE id = :eventId
          AND status IN ('PENDING', 'RETRYING')
    """)
    suspend fun claimEvent(
        eventId: Long,
        updatedAt: Long
    ): Int

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: Long): EventEntity?

    // Update processing status and retry information.
    @Query("""
        UPDATE events
        SET status = :status,
            retryCount = :retryCount,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE id = :eventId
    """)
    suspend fun updateEventStatus(
        eventId: Long,
        status: String,
        retryCount: Int,
        lastError: String?,
        updatedAt: Long
    )

    // Recover events left IN_PROGRESS if a worker was interrupted.
    @Query("""
        UPDATE events
        SET status = 'RETRYING',
            updatedAt = :updatedAt
        WHERE status = 'IN_PROGRESS'
          AND updatedAt < :staleBefore
    """)
    suspend fun recoverStaleEvents(
        staleBefore: Long,
        updatedAt: Long
    )

    // Statistics: successfully processed events only.
    @Query("""
        SELECT * FROM events
        WHERE status = 'PROCESSED'
        ORDER BY timestamp DESC
    """)
    fun observeProcessedEvents(): Flow<List<EventEntity>>

    @Query("""
        SELECT COUNT(DISTINCT sessionId) FROM events
        WHERE eventType = 'VISIT'
          AND status = 'PROCESSED'
          AND sessionId IS NOT NULL
    """)
    fun observeUniqueProcessedVisits(): Flow<Int>

    @Query("""
        SELECT eventType, COUNT(*) AS eventCount
        FROM events
        WHERE status = 'PROCESSED'
        GROUP BY eventType
    """)
    fun observeProcessedCountsByType(): Flow<List<EventTypeCount>>

    @Query("""
    UPDATE events
    SET status = 'PENDING',
        retryCount = 0,
        nextAttemptAt = NULL,
        lastError = NULL,
        updatedAt = :now
    WHERE id = :eventId
      AND status IN ('FAILED', 'RETRYING')
""")
    suspend fun retryEvent(
        eventId: Long,
        now: Long
    ): Int
}
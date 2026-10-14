package com.example.appeventtracker.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the current session ID.
 *
 * The ID is persisted so that process death or app reopening does not
 * automatically create a new session.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        "event_tracker_session",
        Context.MODE_PRIVATE
    )

    /**
     * Returns the existing session ID or creates one if missing.
     */
    fun getCurrentSessionId(): String {
        return preferences.getString(KEY_SESSION_ID, null)
            ?: createNewSession()
    }

    /**
     * Starts a new logical session.
     *
     * This can be called by an explicit "New Session" action.
     */
    @Synchronized
    fun createNewSession(): String {
        val sessionId = UUID.randomUUID().toString()

        preferences.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .apply()

        return sessionId
    }

    private companion object {
        const val KEY_SESSION_ID = "current_session_id"
    }
}
package com.example.appeventtracker.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(
        "event_tracker_session",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun startOrResumeSession(): Boolean {
        val now = System.currentTimeMillis()
        val sessionId = preferences.getString(KEY_SESSION_ID, null)
        val lastBackgroundAt = preferences.getLong(
            KEY_LAST_BACKGROUND_AT,
            0L
        )

        val isNewSession = sessionId == null ||
                lastBackgroundAt == 0L ||
                now - lastBackgroundAt >= SESSION_TIMEOUT_MS

        if (isNewSession) {
            preferences.edit()
                .putString(KEY_SESSION_ID, UUID.randomUUID().toString())
                .remove(KEY_LAST_BACKGROUND_AT)
                .apply()
        } else {
            preferences.edit()
                .remove(KEY_LAST_BACKGROUND_AT)
                .apply()
        }

        return isNewSession
    }

    fun getCurrentSessionId(): String {
        return preferences.getString(KEY_SESSION_ID, null)
            ?: createNewSession()
    }

    @Synchronized
    fun createNewSession(): String {
        val sessionId = UUID.randomUUID().toString()

        preferences.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .remove(KEY_LAST_BACKGROUND_AT)
            .apply()

        return sessionId
    }

    fun markAppBackgrounded() {
        preferences.edit()
            .putLong(
                KEY_LAST_BACKGROUND_AT,
                System.currentTimeMillis()
            )
            .apply()
    }

    private companion object {
        const val KEY_SESSION_ID = "current_session_id"
        const val KEY_LAST_BACKGROUND_AT = "last_background_at"
        const val SESSION_TIMEOUT_MS = 2 * 60 * 1000L
    }
}
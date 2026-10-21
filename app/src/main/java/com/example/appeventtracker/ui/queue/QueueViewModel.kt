package com.example.appeventtracker.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appeventtracker.data.EventEnqueueCoordinator
import com.example.appeventtracker.data.local.EnqueueResult
import com.example.appeventtracker.data.local.EventDao
import com.example.appeventtracker.data.local.EventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QueueFilter {
    IN_PROGRESS,
    FAILED_RETRYING
}

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val enqueueCoordinator: EventEnqueueCoordinator,
    private val eventWorkScheduler: EventWorkScheduler
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(QueueFilter.IN_PROGRESS)
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val queueEvents = eventDao.observeQueueEvents()
    private val failedEvents = eventDao.observeFailedAndRetryingEvents()

    val visibleEvents: StateFlow<List<EventEntity>> = combine(
        queueEvents,
        failedEvents,
        _selectedFilter
    ) { activeEvents, failedEventsList, filter ->
        when (filter) {
            QueueFilter.IN_PROGRESS -> activeEvents

            QueueFilter.FAILED_RETRYING -> failedEventsList
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun selectFilter(filter: QueueFilter) {
        _selectedFilter.value = filter
    }

    fun submitEvents(json: String) {
        if (json.isBlank()) {
            _message.value = "Please enter event JSON."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _message.value = null

            try {
                val results = enqueueCoordinator.enqueueJson(json)

                val added = results.count { it is EnqueueResult.Enqueued }
                val duplicates = results.count { it is EnqueueResult.Duplicate }
                val rejected = results.count { it is EnqueueResult.Rejected }

                _message.value = buildString {
                    append("Added: $added")
                    if (duplicates > 0) append(", duplicates: $duplicates")
                    if (rejected > 0) append(", rejected: $rejected")
                }
            } catch (exception: Exception) {
                _message.value = exception.message ?: "Unable to submit events."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun retryEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val updatedRows = eventDao.retryEvent(
                    eventId = eventId,
                    now = System.currentTimeMillis()
                )

                if (updatedRows > 0) {
                    eventWorkScheduler.scheduleProcessing()
                    _message.value = "Retry scheduled successfully."
                } else {
                    _message.value = "This event is no longer eligible for retry."
                }
            } catch (exception: Exception) {
                _message.value =
                    exception.message ?: "Unable to retry event."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
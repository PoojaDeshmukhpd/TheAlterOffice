package com.example.appeventtracker.ui.stastics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appeventtracker.data.local.EventDao
import com.example.appeventtracker.data.local.EventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class EventBreakdown(
    val eventType: String,
    val count: Int,
    val percentage: Float
)

data class StatisticsUiState(
    val totalProcessedEvents: Int = 0,
    val totalUniqueVisits: Int = 0,
    val installCount: Int = 0,
    val visitCount: Int = 0,
    val addToCartCount: Int = 0,
    val purchaseCount: Int = 0,
    val breakdown: List<EventBreakdown> = emptyList(),
    val processedEvents: List<EventEntity> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    eventDao: EventDao
) : ViewModel() {

    private val processedEvents = eventDao.observeProcessedEvents()
    private val uniqueVisits = eventDao.observeUniqueProcessedVisits()
    private val countsByType = eventDao.observeProcessedCountsByType()

    val uiState: StateFlow<StatisticsUiState> = combine(
        processedEvents,
        uniqueVisits,
        countsByType
    ) { events, visits, typeCounts ->

        val countMap = typeCounts.associate {
            it.eventType to it.eventCount
        }

        val total = events.size

        fun count(type: String): Int = countMap[type] ?: 0

        val eventTypes = listOf(
            "INSTALL",
            "VISIT",
            "ADD_TO_CART",
            "PURCHASE"
        )

        val breakdown = eventTypes.map { type ->
            val eventCount = count(type)

            EventBreakdown(
                eventType = type,
                count = eventCount,
                percentage = if (total > 0) {
                    eventCount * 100f / total
                } else {
                    0f
                }
            )
        }

        StatisticsUiState(
            totalProcessedEvents = total,
            totalUniqueVisits = visits,
            installCount = count("INSTALL"),
            visitCount = count("VISIT"),
            addToCartCount = count("ADD_TO_CART"),
            purchaseCount = count("PURCHASE"),
            breakdown = breakdown,
            processedEvents = events
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
    )
}
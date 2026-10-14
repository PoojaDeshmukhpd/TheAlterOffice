package com.example.appeventtracker.delegates

import com.example.appeventtracker.data.local.EventEntity

interface EventIngestionDelegate {
    suspend fun ingest(event: EventEntity)
}
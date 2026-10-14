package com.example.appeventtracker.ui.queue

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appeventtracker.data.local.EventEntity
import com.example.appeventtracker.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter :
    ListAdapter<EventEntity, EventAdapter.EventViewHolder>(EventDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventEntity) {
            binding.tvEventType.text = event.eventType

            // Format the event timestamp like the reference UI.
            binding.tvEventTimestamp.text = SimpleDateFormat(
                "hh:mm:ss a",
                Locale.getDefault()
            ).format(Date(event.timestamp))

            // Use concise status labels and colors.
            val (statusText, statusColor) = when (event.status) {
                "PROCESSED" -> "Processed ✓" to Color.rgb(22, 163, 74)
                "IN_PROGRESS" -> "Processing" to Color.rgb(37, 99, 255)
                "RETRYING" -> "Retrying" to Color.rgb(249, 115, 22)
                "FAILED" -> "Failed" to Color.rgb(220, 38, 38)
                else -> "In queue" to Color.rgb(107, 114, 128)
            }

            binding.tvEventStatus.text = statusText
            binding.tvEventStatus.setTextColor(statusColor)

            // Give each event type its own indicator color.
            val indicatorColor = when (event.eventType) {
                "INSTALL" -> Color.rgb(220, 252, 231)
                "VISIT" -> Color.rgb(219, 234, 254)
                "ADD_TO_CART" -> Color.rgb(237, 233, 254)
                "PURCHASE" -> Color.rgb(255, 237, 213)
                else -> Color.rgb(229, 231, 235)
            }

            binding.viewEventIndicator.setBackgroundColor(indicatorColor)
            binding.viewEventIndicator.background =
                createCircleDrawable(indicatorColor)
        }

        private fun createCircleDrawable(color: Int): android.graphics.drawable.GradientDrawable {
            return android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
            }
        }
    }

    private object EventDiffCallback : DiffUtil.ItemCallback<EventEntity>() {

        override fun areItemsTheSame(
            oldItem: EventEntity,
            newItem: EventEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: EventEntity,
            newItem: EventEntity
        ): Boolean = oldItem == newItem
    }
}
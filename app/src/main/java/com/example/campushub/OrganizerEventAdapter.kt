package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.domain.CapacityPolicy
import com.example.campushub.domain.EventStatus
import com.example.campushub.model.Event

class OrganizerEventAdapter(
    private val events: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.orgTitle)
        val status: TextView = view.findViewById(R.id.orgStatus)
        val date: TextView = view.findViewById(R.id.orgDate)
        val occupancy: TextView = view.findViewById(R.id.orgOccupancy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_organizer_event, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context

        holder.title.text = event.title
        holder.date.text = event.formattedDate()
        holder.occupancy.text = listOf(
            CapacityPolicy.occupancyLabel(event),
            CapacityPolicy.waitlistLabel(event)
        ).filter { it.isNotEmpty() }.joinToString(" · ")

        holder.status.text = EventStatus.label(event.status)
        when (event.status) {
            EventStatus.DRAFT -> {
                holder.status.setBackgroundResource(R.drawable.bg_badge_neutral)
                holder.status.setTextColor(context.getColor(R.color.text_secondary))
            }
            EventStatus.CLOSED -> {
                holder.status.setBackgroundResource(R.drawable.bg_badge_closed)
                holder.status.setTextColor(context.getColor(R.color.warning))
            }
            else -> {
                holder.status.setBackgroundResource(R.drawable.bg_badge_public)
                holder.status.setTextColor(context.getColor(R.color.success))
            }
        }

        holder.itemView.setOnClickListener { onClick(event) }
    }

    override fun getItemCount() = events.size
}
package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campushub.model.Event

class EventAdapter(
    private val events: List<Event>,
    private val favoriteIds: MutableSet<String>,
    private val onFavoriteChanged: (Event, Boolean) -> Unit,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val favorite: ImageView = view.findViewById(R.id.itemFavorite)
        val course: TextView = view.findViewById(R.id.itemCourse)
        val date: TextView = view.findViewById(R.id.itemDate)
        val location: TextView = view.findViewById(R.id.itemLocation)
        val badge: TextView = view.findViewById(R.id.itemBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context

        holder.title.text = event.title
        holder.course.text =
            if (event.category.isNotEmpty()) "${event.category} · ${event.hostCourse}"
            else event.hostCourse
        holder.date.text = event.formattedDate() + if (event.isFinished()) " · Encerrado" else ""
        holder.location.text = event.location

        if (event.isPublic) {
            holder.badge.text = "Público"
            holder.badge.setBackgroundResource(R.drawable.bg_badge_public)
            holder.badge.setTextColor(context.getColor(R.color.success))
        } else {
            holder.badge.text = "Fechado"
            holder.badge.setBackgroundResource(R.drawable.bg_badge_closed)
            holder.badge.setTextColor(context.getColor(R.color.warning))
        }

        val isFavorite = event.id in favoriteIds
        holder.favorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.favorite.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val nowFavorite = event.id !in favoriteIds
            if (nowFavorite) favoriteIds.add(event.id) else favoriteIds.remove(event.id)
            notifyItemChanged(adapterPosition)
            onFavoriteChanged(event, nowFavorite)
        }

        holder.itemView.setOnClickListener { onClick(event) }
    }

    override fun getItemCount() = events.size
}
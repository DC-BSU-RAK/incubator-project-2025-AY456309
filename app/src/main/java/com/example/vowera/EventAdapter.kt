package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private val events: MutableList<Event>,
    private val onEdit: (Event) -> Unit,
    private val onDelete: (Event) -> Unit,
    private val onItemClick: (Event) -> Unit = {}
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvEventBudget: TextView = itemView.findViewById(R.id.tvEventBudget)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.tvEventName.text = event.name
        holder.tvEventDate.text = "Date: ${event.date.ifEmpty { "Not set" }}"
        holder.tvEventLocation.text = "Location: ${event.location.ifEmpty { "Not set" }}"
        holder.tvEventBudget.text = "Budget: ${event.budget}"

        // 👉 Whole item click (opens event details)
        holder.itemView.setOnClickListener {
            onItemClick(event)
        }

        // 👉 Edit button (ONLY edit, no navigation)
        holder.btnEdit.setOnClickListener {
            it.isClickable = true
            it.isFocusable = true
            it.parent.requestDisallowInterceptTouchEvent(true)
            onEdit(event)
        }

        // 👉 Delete button (ONLY delete, no navigation)
        holder.btnDelete.setOnClickListener {
            it.isClickable = true
            it.isFocusable = true
            it.parent.requestDisallowInterceptTouchEvent(true)
            onDelete(event)
        }
    }

    override fun getItemCount(): Int = events.size
}
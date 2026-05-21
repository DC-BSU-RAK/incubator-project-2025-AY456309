package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class GuestAdapter : ListAdapter<Guest, GuestAdapter.GuestViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Guest>() {
            override fun areItemsTheSame(oldItem: Guest, newItem: Guest): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: Guest, newItem: Guest): Boolean {
                return oldItem == newItem
            }
        }
    }

    class GuestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGuestName: TextView = itemView.findViewById(R.id.tvGuestName)
        val tvGuestRsvp: TextView = itemView.findViewById(R.id.tvGuestRsvp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guest, parent, false)
        return GuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        val guest = getItem(position)
        holder.tvGuestName.text = guest.name
        holder.tvGuestRsvp.text = guest.rsvp
    }
}

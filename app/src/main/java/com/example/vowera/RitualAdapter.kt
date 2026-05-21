package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class RitualAdapter : ListAdapter<Ritual, RitualAdapter.RitualViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Ritual>() {
            override fun areItemsTheSame(oldItem: Ritual, newItem: Ritual): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: Ritual, newItem: Ritual): Boolean {
                return oldItem == newItem
            }
        }
    }

    class RitualViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRitualName: TextView = itemView.findViewById(R.id.tvRitualName)
        val tvRitualTime: TextView = itemView.findViewById(R.id.tvRitualTime)
        val tvRitualDescription: TextView = itemView.findViewById(R.id.tvRitualDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RitualViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ritual, parent, false)
        return RitualViewHolder(view)
    }

    override fun onBindViewHolder(holder: RitualViewHolder, position: Int) {
        val ritual = getItem(position)
        holder.tvRitualName.text = ritual.name
        holder.tvRitualTime.text = "Time: ${ritual.time}"
        holder.tvRitualDescription.text = ritual.description
    }
}

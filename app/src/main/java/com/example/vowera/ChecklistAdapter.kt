package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class ChecklistAdapter(
    private val onItemChecked: (ChecklistItem) -> Unit
) : ListAdapter<ChecklistItem, ChecklistAdapter.ChecklistViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChecklistItem>() {
            override fun areItemsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ChecklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbChecklistItem: CheckBox = itemView.findViewById(R.id.cbChecklistItem)
        val tvChecklistItem: TextView = itemView.findViewById(R.id.tvChecklistItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvChecklistItem.text = item.item
        holder.cbChecklistItem.isChecked = item.completed
        holder.cbChecklistItem.setOnCheckedChangeListener { _, isChecked ->
            item.completed = isChecked
            onItemChecked(item)
        }
    }
}

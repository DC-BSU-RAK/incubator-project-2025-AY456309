package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(
    private val list: MutableList<TimelineItem>,
    private val onItemAction: (TimelineItem, String) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView? = view.findViewById(R.id.titleText)
        val date: TextView? = view.findViewById(R.id.dateText)
        val details: TextView? = view.findViewById(R.id.detailsText)
        val edit: ImageView? = view.findViewById(R.id.editBtn)
        val delete: ImageView? = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.title?.text = item.title
        holder.date?.text = "Date: ${item.date}"
        holder.details?.text = "Details: ${item.details}"

        holder.delete?.setOnClickListener {
            onItemAction(item, "delete")
        }

        holder.edit?.setOnClickListener {
            onItemAction(item, "edit")
        }
    }

    // Enhanced methods for better list management
    fun updateList(newList: List<TimelineItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun addItem(item: TimelineItem) {
        list.add(0, item)
        notifyItemInserted(0)
    }

    fun removeItem(item: TimelineItem) {
        val index = list.indexOf(item)
        if (index != -1) {
            list.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateItem(item: TimelineItem) {
        val index = list.indexOfFirst { it.id == item.id }
        if (index != -1) {
            list[index] = item
            notifyItemChanged(index)
        }
    }
}
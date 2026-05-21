package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class WeddingChecklistCategoryAdapter(
    private val onItemChecked: (ChecklistItem) -> Unit,
    private val onItemDelete: (ChecklistItem) -> Unit
) : ListAdapter<ChecklistCategory, WeddingChecklistCategoryAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChecklistCategory>() {
            override fun areItemsTheSame(oldItem: ChecklistCategory, newItem: ChecklistCategory): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: ChecklistCategory, newItem: ChecklistCategory): Boolean {
                return oldItem == newItem
            }
        }
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView? = itemView.findViewById(R.id.tvChecklistCategoryName)
        val containerTasks: LinearLayout? = itemView.findViewById(R.id.containerChecklistTasks)
        val tvTaskCount: TextView? = itemView.findViewById(R.id.tvTaskCount)
        val ivExpandCollapse: ImageView? = itemView.findViewById(R.id.ivExpandCollapse)

        fun bind(category: ChecklistCategory, onItemChecked: (ChecklistItem) -> Unit, onItemDelete: (ChecklistItem) -> Unit) {
            try {
                tvCategoryName?.text = category.name

                val completedCount = category.items.count { it.completed }
                tvTaskCount?.text = "$completedCount / ${category.items.size}"

                containerTasks?.removeAllViews()

                var isExpanded = true
                ivExpandCollapse?.setOnClickListener {
                    isExpanded = !isExpanded
                    containerTasks?.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    ivExpandCollapse?.rotation = if (isExpanded) 0f else -180f
                }

                for (item in category.items) {
                    try {
                        val itemView = LayoutInflater.from(itemView.context)
                            .inflate(R.layout.item_wedding_checklist, containerTasks, false)

                        if (itemView != null) {
                            val cbItem = itemView.findViewById<CheckBox>(R.id.cbChecklistItem)
                            val tvItem = itemView.findViewById<TextView>(R.id.tvChecklistItem)
                            val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteChecklistItem)

                            cbItem?.isChecked = item.completed
                            tvItem?.text = item.item

                            if (item.completed) {
                                tvItem?.alpha = 0.6f
                            }

                            cbItem?.setOnCheckedChangeListener { _, isChecked ->
                                item.completed = isChecked
                                tvItem?.alpha = if (isChecked) 0.6f else 1.0f
                                onItemChecked(item)
                            }

                            btnDelete?.setOnClickListener {
                                onItemDelete(item)
                            }

                            containerTasks?.addView(itemView)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checklist_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), onItemChecked, onItemDelete)
    }
}


package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShoppingListAdapter(
    private val items: MutableList<ShoppingListItem>,
    private val onEdit: (ShoppingListItem) -> Unit,
    private val onDelete: (ShoppingListItem) -> Unit,
    private val onItemChecked: (ShoppingListItem, Boolean) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    inner class ShoppingListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ShoppingListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        val item = items[position]

        holder.tvItemName.text = item.name
        holder.tvQuantity.text = "Qty: ${item.quantity}"
        holder.tvCategory.text = item.category

        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = item.isCompleted

        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            onItemChecked(item, isChecked)
        }

        holder.btnEdit.setOnClickListener {
            onEdit(item)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

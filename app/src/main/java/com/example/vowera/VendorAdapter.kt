package com.example.vowera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VendorAdapter(
    private val vendors: MutableList<Vendor>,
    private val onEdit: (Vendor) -> Unit,
    private val onDelete: (Vendor) -> Unit,
    private val onItemClick: (Vendor) -> Unit = {}
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    inner class VendorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVendorName: TextView = itemView.findViewById(R.id.tvVendorName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvContactPerson: TextView = itemView.findViewById(R.id.tvContactPerson)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor, parent, false)
        return VendorViewHolder(view)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        val vendor = vendors[position]
        holder.tvVendorName.text = vendor.name
        holder.tvCategory.text = vendor.category
        holder.tvContactPerson.text = "Contact: ${vendor.contactPerson}"
        holder.tvPhone.text = vendor.phone

        holder.itemView.setOnClickListener {
            onItemClick(vendor)
        }

        holder.btnEdit.setOnClickListener {
            onEdit(vendor)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(vendor)
        }
    }

    override fun getItemCount(): Int = vendors.size

    fun updateList(newList: List<Vendor>) {
        vendors.clear()
        vendors.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(vendor: Vendor) {
        val index = vendors.indexOf(vendor)
        if (index != -1) {
            vendors.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addItem(vendor: Vendor) {
        vendors.add(0, vendor)
        notifyItemInserted(0)
    }

    fun updateItem(vendor: Vendor) {
        val index = vendors.indexOfFirst { it.id == vendor.id }
        if (index != -1) {
            vendors[index] = vendor
            notifyItemChanged(index)
        }
    }
}


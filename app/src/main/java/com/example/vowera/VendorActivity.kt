package com.example.vowera

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvVendors: RecyclerView
    private lateinit var adapter: VendorAdapter
    private val vendors = mutableListOf<Vendor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupNavigation()
        loadVendors()
    }

    private fun setupUI() {
        rvVendors = findViewById(R.id.rvVendors)
        val fabAddVendor = findViewById<FloatingActionButton>(R.id.fabAddVendor)

        rvVendors.layoutManager = LinearLayoutManager(this)

        adapter = VendorAdapter(
            vendors,
            onEdit = { vendor -> showEditDialog(vendor) },
            onDelete = { vendor -> deleteVendor(vendor) }
        )

        rvVendors.adapter = adapter

        fabAddVendor.setOnClickListener {
            showAddDialog()
        }
    }

    private fun setupNavigation() {
        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)

        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)

        btnNotif.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        btnMore.setOnClickListener {
            showMoreMenu(it)
        }

        navDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        navEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
            finish()
        }

        navTimeline.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, TimelineActivity::class.java))
            finish()
        }

        navBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
            finish()
        }

        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
        }
    }

    private fun loadVendors() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .collection("vendor")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load vendors", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(Vendor::class.java)
                    adapter.updateList(items)
                }
            }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_vendor, null)
        val etVendorName = dialogView.findViewById<EditText>(R.id.etVendorName)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etContactPerson = dialogView.findViewById<EditText>(R.id.etContactPerson)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etCost = dialogView.findViewById<EditText>(R.id.etCost)

        AlertDialog.Builder(this)
            .setTitle("Add Vendor")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etVendorName.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val contactPerson = etContactPerson.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val cost = etCost.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Vendor name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addVendor(name, category.ifEmpty { "Other" }, contactPerson, phone, email, cost)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(vendor: Vendor) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_vendor, null)
        val etVendorName = dialogView.findViewById<EditText>(R.id.etVendorName)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etContactPerson = dialogView.findViewById<EditText>(R.id.etContactPerson)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etCost = dialogView.findViewById<EditText>(R.id.etCost)

        etVendorName.setText(vendor.name)
        etCategory.setText(vendor.category)
        etContactPerson.setText(vendor.contactPerson)
        etPhone.setText(vendor.phone)
        etEmail.setText(vendor.email)
        etCost.setText(vendor.cost)

        AlertDialog.Builder(this)
            .setTitle("Edit Vendor")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val name = etVendorName.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val contactPerson = etContactPerson.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val cost = etCost.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Vendor name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateVendor(vendor, name, category.ifEmpty { "Other" }, contactPerson, phone, email, cost)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addVendor(name: String, category: String, contactPerson: String, phone: String, email: String, cost: String) {
        val currentUser = auth.currentUser ?: return

        val newVendor = Vendor(
            id = db.collection("users").document(currentUser.uid)
                .collection("vendor").document().id,
            name = name,
            category = category,
            contactPerson = contactPerson,
            phone = phone,
            email = email,
            cost = cost,
            status = "pending"
        )

        db.collection("users").document(currentUser.uid)
            .collection("vendor").document(newVendor.id)
            .set(newVendor)
            .addOnSuccessListener {
                Toast.makeText(this, "Vendor added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add vendor: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateVendor(vendor: Vendor, name: String, category: String, contactPerson: String, phone: String, email: String, cost: String) {
        val currentUser = auth.currentUser ?: return

        val updatedVendor = vendor.copy(
            name = name,
            category = category,
            contactPerson = contactPerson,
            phone = phone,
            email = email,
            cost = cost
        )

        db.collection("users").document(currentUser.uid)
            .collection("vendor").document(vendor.id)
            .set(updatedVendor)
            .addOnSuccessListener {
                Toast.makeText(this, "Vendor updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update vendor: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteVendor(vendor: Vendor) {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .collection("vendor").document(vendor.id)
            .delete()
            .addOnSuccessListener {
                adapter.removeItem(vendor)
                Toast.makeText(this, "Vendor deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete vendor: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMoreMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menu.add(0, 1, 0, "Settings")
        popupMenu.menu.add(0, 2, 1, "Unlock the full version")
        popupMenu.menu.add(0, 3, 2, "Logout")

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                2 -> {
                    startActivity(Intent(this, PremiumActivity::class.java))
                    true
                }
                3 -> {
                    auth.signOut()
                    finish()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
}

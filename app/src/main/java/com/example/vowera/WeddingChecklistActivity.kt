package com.example.vowera

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class WeddingChecklistActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: WeddingChecklistCategoryAdapter
    private lateinit var uid: String
    private val categories = mutableListOf<ChecklistCategory>()

    // FIX: Added eventName variable to match the Dashboard's Intent and clearAllTasks logic
    private var eventName: String = "Wedding"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_wedding_checklist)

            // FIX: Retrieve the eventName passed from DashboardActivity
            eventName = intent.getStringExtra("eventName") ?: "Wedding"

            val mainView = findViewById<View>(R.id.main)
            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            uid = currentUser.uid

            setupNavigation()
            setupChecklist()
            setupButtons()
            loadChecklistData()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing checklist: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun setupChecklist() {
        try {
            val rvChecklist = findViewById<RecyclerView>(R.id.rvWeddingChecklist)
            if (rvChecklist == null) {
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            rvChecklist.layoutManager = LinearLayoutManager(this)

            adapter = WeddingChecklistCategoryAdapter(
                onItemChecked = { item -> updateChecklistItem(item) },
                onItemDelete = { item -> deleteChecklistItem(item) }
            )

            rvChecklist.adapter = adapter
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up checklist: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        try {
            val btnAddTask = findViewById<Button>(R.id.btnAddTask)
            if (btnAddTask != null) {
                btnAddTask.setOnClickListener {
                    showAddTaskDialog()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up buttons: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun loadChecklistData() {
        // FIX: Use SnapshotListener like VendorActivity for real-time updates
        db.collection("users").document(uid)
            .collection("wedding_checklist")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    categories.clear()
                    val categoryMap = mutableMapOf<String, MutableList<ChecklistItem>>()

                    for (doc in querySnapshot.documents) {
                        val item = doc.toObject(ChecklistItem::class.java)
                        if (item != null) {
                            val itemWithId = item.copy(id = doc.id)
                            val category = doc.getString("category") ?: "Other"
                            if (!categoryMap.containsKey(category)) categoryMap[category] = mutableListOf()
                            categoryMap[category]?.add(itemWithId)
                        }
                    }

                    val defaultCategories = listOf(
                        "Venue & Catering", "Guest List", "Decorations", "Photography/Video",
                        "Music & Entertainment", "Attire", "Rituals & Ceremonies",
                        "Vendors", "Budget & Payments", "Other"
                    )

                    for (catName in defaultCategories) {
                        val items = categoryMap[catName] ?: mutableListOf()
                        categories.add(ChecklistCategory(catName, items))
                    }

                    adapter.submitList(categories.toList())
                    updateProgressBar()
                }
            }
    }

    private fun showAddTaskDialog() {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_checklist_item, null)
            if (dialogView == null) {
                Toast.makeText(this, "Error: Could not load dialog layout", Toast.LENGTH_SHORT).show()
                return
            }

            val etItemName = dialogView.findViewById<EditText>(R.id.etChecklistItemName)
            val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerChecklistCategory)

            if (etItemName == null || spinnerCategory == null) {
                Toast.makeText(this, "Error: Dialog views not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Setup category spinner
            val categoryList = listOf(
                "Venue & Catering",
                "Guest List",
                "Decorations",
                "Photography/Video",
                "Music & Entertainment",
                "Attire",
                "Rituals & Ceremonies",
                "Vendors",
                "Budget & Payments",
                "Other"
            )
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = categoryAdapter

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    try {
                        val itemName = etItemName.text.toString().trim()
                        val selectedCategory = spinnerCategory.selectedItem.toString()

                        if (itemName.isNotEmpty()) {
                            addChecklistItem(itemName, selectedCategory)
                        } else {
                            Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error adding item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening dialog: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun addChecklistItem(itemName: String, category: String) {

        val docRef = db.collection("users").document(uid)
            .collection("wedding_checklist").document()

        val itemData = mapOf(
            "item" to itemName,
            "completed" to false,
            "category" to category,
            "createdAt" to System.currentTimeMillis()
        )

        docRef.set(itemData)
            .addOnSuccessListener {
                Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show()
                // FIX: Remove loadChecklistData() call - SnapshotListener handles real-time updates
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add task: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateChecklistItem(item: ChecklistItem) {
        db.collection("users").document(uid)
            .collection("wedding_checklist").document(item.id)
            .update("completed", item.completed)
            .addOnSuccessListener {
                updateProgressBar()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteChecklistItem(item: ChecklistItem) {

        db.collection("users").document(uid)
            .collection("wedding_checklist").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                // FIX: Remove loadChecklistData() call - SnapshotListener handles real-time updates
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProgressBar() {
        try {
            val progressBar = findViewById<ProgressBar>(R.id.progressWeddingChecklist)
            val tvProgress = findViewById<TextView>(R.id.tvChecklistProgress)

            if (progressBar != null && tvProgress != null) {
                var totalItems = 0
                var completedItems = 0

                for (category in categories) {
                    for (item in category.items) {
                        totalItems++
                        if (item.completed) {
                            completedItems++
                        }
                    }
                }

                val progress = if (totalItems > 0) (completedItems * 100) / totalItems else 0
                progressBar.progress = progress
                tvProgress.text = String.format(Locale.getDefault(), "%d / %d Items Completed", completedItems, totalItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupNavigation() {
        try {
            val btnNotif = findViewById<ImageView>(R.id.btnNotif)
            val btnMore = findViewById<ImageView>(R.id.btnMore)

            val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
            val navEvents = findViewById<LinearLayout>(R.id.navEvents)
            val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
            val navBudget = findViewById<LinearLayout>(R.id.navBudget)
            val navGuests = findViewById<LinearLayout>(R.id.navGuests)

            btnNotif?.setOnClickListener {
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
            }

            btnMore?.setOnClickListener {
                showMoreMenu(it)
            }

            navDashboard?.setOnClickListener {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }

            navEvents?.setOnClickListener {
                startActivity(Intent(this, EventsActivity::class.java))
                finish()
            }

            navTimeline?.setOnClickListener {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                startActivity(Intent(this, TimelineActivity::class.java))
                finish()
            }

            navBudget?.setOnClickListener {
                startActivity(Intent(this, BudgetActivity::class.java))
                finish()
            }

            navGuests?.setOnClickListener {
                startActivity(Intent(this, GuestListActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up navigation: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showMoreMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menu.add(0, 1, 0, "Settings")
        popupMenu.menu.add(0, 2, 1, "Clear All Tasks")
        popupMenu.menu.add(0, 3, 2, "Logout")

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                2 -> {
                    showClearAllConfirmation()
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

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Tasks")
            .setMessage("Are you sure you want to delete all tasks? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                clearAllTasks()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearAllTasks() {
        try {
            // FIX: Remove unused sanitizedName variable
            db.collection("users").document(uid)
                .collection("wedding_checklist").get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        for (doc in querySnapshot.documents) {
                            doc.reference.delete()
                        }
                        Toast.makeText(this, "All tasks cleared", Toast.LENGTH_SHORT).show()
                        // FIX: Remove loadChecklistData() call - SnapshotListener handles real-time updates
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error clearing tasks: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to clear tasks: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}

data class ChecklistCategory(
    val name: String,
    val items: MutableList<ChecklistItem>
)

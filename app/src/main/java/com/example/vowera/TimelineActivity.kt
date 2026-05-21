package com.example.vowera

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class TimelineActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var timelineRecycler: RecyclerView
    private lateinit var addBtn: FloatingActionButton
    private lateinit var timelineAdapter: TimelineAdapter
    private val timelineItems = mutableListOf<TimelineItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_timeline)
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

            timelineRecycler = findViewById(R.id.timelineRecycler)
            if (timelineRecycler == null) {
                Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            addBtn = findViewById(R.id.addBtn)
            if (addBtn == null) {
                Toast.makeText(this, "Error: FAB not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val btnNotif = findViewById<ImageView>(R.id.btnNotif)
            val btnMore = findViewById<ImageView>(R.id.btnMore)

            val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
            val navEvents = findViewById<LinearLayout>(R.id.navEvents)
            val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
            val navBudget = findViewById<LinearLayout>(R.id.navBudget)
            val navGuests = findViewById<LinearLayout>(R.id.navGuests)

            timelineAdapter = TimelineAdapter(timelineItems, { item, action ->
                when (action) {
                    "edit" -> showEditTimelineDialog(item)
                    "delete" -> showDeleteTimelineDialog(item)
                }
            })

            timelineRecycler.layoutManager = LinearLayoutManager(this)
            timelineRecycler.adapter = timelineAdapter

            addBtn.setOnClickListener { showAddTimelineDialog() }

            btnNotif?.setOnClickListener {
                Toast.makeText(this, getString(R.string.notifications_coming_soon), Toast.LENGTH_SHORT).show()
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
                // Stay on current screen - do nothing
            }

            navBudget?.setOnClickListener {
                startActivity(Intent(this, BudgetActivity::class.java))
                finish()
            }

            navGuests?.setOnClickListener {
                startActivity(Intent(this, GuestListActivity::class.java))
                finish()
            }

            loadTimeline()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing timeline: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun loadTimeline() {
        try {
            val currentUser = auth.currentUser ?: return
            db.collection("users").document(currentUser.uid)
                .collection("checklist")
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        timelineItems.clear()
                        for (doc in documents) {
                            try {
                                val item = doc.toObject(TimelineItem::class.java)
                                if (item != null) {
                                    timelineItems.add(item.copy(id = doc.id))
                                }
                            } catch (e: Exception) {
                                // Log but continue with other items
                                e.printStackTrace()
                            }
                        }
                        timelineAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error processing timeline items: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load timeline: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading timeline: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAddTimelineDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_timeline, null)
            if (dialogView == null) {
                Toast.makeText(this, "Error: Could not load dialog layout", Toast.LENGTH_SHORT).show()
                return
            }

            val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
            val etDate = dialogView.findViewById<EditText>(R.id.etDate)
            val etDetails = dialogView.findViewById<EditText>(R.id.etDetails)

            if (etTitle == null || etDate == null || etDetails == null) {
                Toast.makeText(this, "Error: Dialog views not found", Toast.LENGTH_SHORT).show()
                return
            }

            etDate.setOnClickListener {
                try {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(this, { _, year, month, day ->
                        etDate.setText("$day/${month + 1}/$year")
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening date picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Add Timeline Item")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    try {
                        val title = etTitle.text.toString().trim()
                        val date = etDate.text.toString().trim()
                        val details = etDetails.text.toString().trim()
                        if (title.isNotEmpty()) {
                            addTimelineItem(TimelineItem(title = title, date = date, details = details))
                        } else {
                            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error adding item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening dialog: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showEditTimelineDialog(item: TimelineItem) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_timeline, null)
            if (dialogView == null) {
                Toast.makeText(this, "Error: Could not load dialog layout", Toast.LENGTH_SHORT).show()
                return
            }

            val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
            val etDate = dialogView.findViewById<EditText>(R.id.etDate)
            val etDetails = dialogView.findViewById<EditText>(R.id.etDetails)

            if (etTitle == null || etDate == null || etDetails == null) {
                Toast.makeText(this, "Error: Dialog views not found", Toast.LENGTH_SHORT).show()
                return
            }

            etTitle.setText(item.title)
            etDate.setText(item.date)
            etDetails.setText(item.details)

            etDate.setOnClickListener {
                try {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(this, { _, year, month, day ->
                        etDate.setText("$day/${month + 1}/$year")
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening date picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            AlertDialog.Builder(this)
                .setTitle("Edit Timeline Item")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    try {
                        val title = etTitle.text.toString().trim()
                        val date = etDate.text.toString().trim()
                        val details = etDetails.text.toString().trim()
                        if (title.isNotEmpty()) {
                            updateTimelineItem(item.copy(title = title, date = date, details = details))
                        } else {
                            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error saving item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening edit dialog: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showDeleteTimelineDialog(item: TimelineItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Timeline Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTimelineItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTimelineItem(item: TimelineItem) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .collection("checklist")
            .add(item)
            .addOnSuccessListener { docRef ->
                // Mark timeline as completed when first item is added
                db.collection("users").document(currentUser.uid)
                    .update("timelineCompleted", true)
                    .addOnSuccessListener {
                        // Add the item with the generated ID to the adapter
                        val itemWithId = item.copy(id = docRef.id)
                        timelineItems.add(0, itemWithId)
                        timelineAdapter.addItem(itemWithId)
                        Toast.makeText(this, "Timeline item added", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        loadTimeline()
                        Toast.makeText(this, "Timeline item added but failed to update status: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTimelineItem(item: TimelineItem) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .collection("checklist")
            .document(item.id)
            .set(item)
            .addOnSuccessListener {
                timelineAdapter.updateItem(item)
                Toast.makeText(this, "Timeline item updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTimelineItem(item: TimelineItem) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .collection("checklist")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                loadTimeline()
                Toast.makeText(this, "Timeline item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
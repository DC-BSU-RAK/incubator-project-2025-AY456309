package com.example.vowera
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
class EventsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvEvents: RecyclerView
    private lateinit var fabAddEvent: FloatingActionButton
    private lateinit var eventAdapter: EventAdapter
    private val events = mutableListOf<Event>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        rvEvents = findViewById(R.id.rvEvents)
        fabAddEvent = findViewById(R.id.fabAddEvent)
        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)
        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)
        eventAdapter = EventAdapter(
            events,
            { event ->
                // Add this Toast to check if edit is clicked
                Toast.makeText(this, "Edit clicked for ${event.name}", Toast.LENGTH_SHORT).show()
                showEditEventDialog(event)
            },
            { event -> showDeleteEventDialog(event) },
            { event -> openEventDetails(event) }
        )

        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.adapter = eventAdapter
        fabAddEvent.setOnClickListener { showAddEventDialog() }
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
            Toast.makeText(this, "Events", Toast.LENGTH_SHORT).show()
        }
        navTimeline.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For now, just navigate to TimelineActivity
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
        loadEvents()
    }
    private fun loadEvents() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .collection("event")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load events: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    events.clear()
                    for (document in snapshot.documents) {
                        val event = document.toObject(Event::class.java)?.copy(id = document.id)
                        if (event != null) {
                            events.add(event)
                        }
                    }
                    eventAdapter.notifyDataSetChanged()
                }
            }
    }
    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEventName)
        val etDate = dialogView.findViewById<EditText>(R.id.etEventDate)
        val etLocation = dialogView.findViewById<AutoCompleteTextView>(R.id.etEventLocation)
        val etBudget = dialogView.findViewById<EditText>(R.id.etEventBudget)
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                etDate.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        setupLocationAutocomplete(etLocation)
        AlertDialog.Builder(this)
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val date = etDate.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty()) {
                    addEvent(Event(name = name, date = date, location = location, budget = budget))
                } else {
                    Toast.makeText(this, "Event name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showEditEventDialog(event: Event) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEventName)
        val etDate = dialogView.findViewById<EditText>(R.id.etEventDate)
        val etLocation = dialogView.findViewById<AutoCompleteTextView>(R.id.etEventLocation)
        val etBudget = dialogView.findViewById<EditText>(R.id.etEventBudget)
        etName.setText(event.name)
        etDate.setText(event.date)
        etLocation.setText(event.location)
        etBudget.setText(event.budget.toString())
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                etDate.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        setupLocationAutocomplete(etLocation)
        AlertDialog.Builder(this)
            .setTitle("Edit Event")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val date = etDate.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty()) {
                    updateEvent(event.copy(name = name, date = date, location = location, budget = budget))
                } else {
                    Toast.makeText(this, "Event name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showDeleteEventDialog(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun addEvent(event: Event) {
        val currentUser = auth.currentUser ?: return
        val sanitizedName = sanitizeEventName(event.name)
        db.collection("users").document(currentUser.uid)
            .collection("event").document(sanitizedName).set(event.copy(id = event.name))
            .addOnSuccessListener {
                Toast.makeText(this@EventsActivity, "Event added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateEvent(updatedEvent: Event) {
        val currentUser = auth.currentUser ?: return
        val sanitizedId = sanitizeEventName(updatedEvent.id)
        val sanitizedName = sanitizeEventName(updatedEvent.name)
        if (updatedEvent.name != updatedEvent.id) {
            db.collection("users").document(currentUser.uid).collection("event")
                .document(sanitizedId).delete()
                .addOnSuccessListener {
                    db.collection("users").document(currentUser.uid).collection("event")
                        .document(sanitizedName).set(updatedEvent.copy(id = updatedEvent.name))
                        .addOnSuccessListener {
                            Toast.makeText(this@EventsActivity, "Event updated", Toast.LENGTH_SHORT).show()
                        }
                }
        } else {
            db.collection("users").document(currentUser.uid).collection("event")
                .document(sanitizedId).set(updatedEvent)
                .addOnSuccessListener {
                    Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun deleteEvent(event: Event) {
        val currentUser = auth.currentUser ?: return
        val sanitizedId = sanitizeEventName(event.id)
        db.collection("users").document(currentUser.uid).collection("event")
            .document(sanitizedId).delete()
            .addOnSuccessListener {
                db.collection("users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val currentEvents = doc.get("events") as? MutableList<String> ?: mutableListOf()
                        currentEvents.remove(event.name)
                        db.collection("users").document(currentUser.uid)
                            .update("events", currentEvents)
                            .addOnSuccessListener {
                                loadEvents()
                                Toast.makeText(this@EventsActivity, "Event deleted", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun setupLocationAutocomplete(etLocation: AutoCompleteTextView) {
        val commonLocations = arrayOf(
            "Hotel Ballroom",
            "Country Club",
            "Beach Resort",
            "Garden Venue",
            "Banquet Hall",
            "Community Center",
            "Restaurant",
            "Home",
            "Park",
            "Outdoor Venue",
            "Church",
            "Temple",
            "Mosque",
            "Gurdwara",
            "Reception Hall",
            "Wedding Hall",
            "Convention Center",
            "Theatre"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, commonLocations)
        etLocation.setAdapter(adapter)
        etLocation.threshold = 1
    }
    private fun openEventDetails(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java)
        intent.putExtra("eventName", event.name)
        startActivity(intent)
    }
    private fun sanitizeEventName(name: String): String {
        return name.replace("/", "_")
            .replace("\\", "_")
            .replace(" ", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("$", "_")
            .replace("?", "_")
            .replace("*", "_")
            .replace("|", "_")
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

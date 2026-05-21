package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EventSetupActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val events = mutableListOf<String>()
    private lateinit var eventsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_setup)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val culture = intent.getStringExtra("culture") ?: ""
        val yourName = intent.getStringExtra("yourName") ?: ""
        val partnerName = intent.getStringExtra("partnerName") ?: ""
        val role = intent.getStringExtra("role") ?: ""
        val prefilled = intent.getStringArrayListExtra("ceremonies") ?: arrayListOf()

        eventsContainer = findViewById(R.id.eventsContainer)
        val addCustomBtn = findViewById<Button>(R.id.btnAddCustomEvent)
        val btnNext = findViewById<Button>(R.id.btnEventNext)
        val cultureLabel = findViewById<TextView>(R.id.cultureLabel)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        cultureLabel.text = "Ceremonies for $culture"

        loadSavedEvents(prefilled)

        addCustomBtn.setOnClickListener {
            showAddEventDialog()
        }

        btnNext.setOnClickListener {
            if (events.isEmpty()) {
                Toast.makeText(this, "Add at least one event", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            btnNext.isEnabled = false
            btnNext.text = "SAVING..."

            db.collection("users").document(uid)
                .set(
                    hashMapOf(
                        "culture" to culture,
                        "events" to events,
                        "eventCount" to events.size,
                        "eventsCompleted" to true
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    val intent = Intent(this, BudgetSetupActivity::class.java)
                    intent.putStringArrayListExtra("events", ArrayList(events))
                    intent.putExtra("yourName", yourName)
                    intent.putExtra("partnerName", partnerName)
                    intent.putExtra("role", role)
                    startActivity(intent)
                    finish()
                }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, CultureSelectionActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@EventSetupActivity, CultureSelectionActivity::class.java))
            finish()
        }
    }

    private fun loadSavedEvents(prefilled: ArrayList<String>) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val savedEvents = doc.get("events") as? List<String>
                events.clear()

                when {
                    !savedEvents.isNullOrEmpty() -> events.addAll(savedEvents)
                    prefilled.isNotEmpty() -> events.addAll(prefilled)
                }

                renderEventList()
            }
    }

    private fun renderEventList() {
        eventsContainer.removeAllViews()

        events.forEachIndexed { index, eventName ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_event_row, eventsContainer, false)

            val nameText = row.findViewById<TextView>(R.id.eventRowName)
            val renameBtn = row.findViewById<ImageView>(R.id.btnRenameEvent)
            val removeBtn = row.findViewById<ImageView>(R.id.btnRemoveEvent)

            nameText.text = eventName

            renameBtn.setOnClickListener {
                showRenameDialog(index, eventName)
            }

            removeBtn.setOnClickListener {
                events.removeAt(index)
                renderEventList()
            }

            eventsContainer.addView(row)
        }
    }

    private fun showRenameDialog(index: Int, currentName: String) {
        val input = EditText(this)
        input.setText(currentName)
        input.background = getDrawable(R.drawable.input_bg)
        input.setPadding(32, 20, 32, 20)

        AlertDialog.Builder(this)
            .setTitle("Rename Event")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    events[index] = newName
                    renderEventList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddEventDialog() {
        val input = EditText(this)
        input.hint = "Event name"
        input.background = getDrawable(R.drawable.input_bg)
        input.setPadding(32, 20, 32, 20)

        AlertDialog.Builder(this)
            .setTitle("Add Custom Event")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    events.add(name)
                    renderEventList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
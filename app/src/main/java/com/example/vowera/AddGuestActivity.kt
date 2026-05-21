package com.example.vowera

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddGuestActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val selectedEvents = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_guest)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val nameInput   = findViewById<EditText>(R.id.guestNameInput)
        val phoneInput  = findViewById<EditText>(R.id.guestPhoneInput)
        val sideSpinner = findViewById<Spinner>(R.id.guestSideSpinner)
        val rsvpSpinner = findViewById<Spinner>(R.id.guestRsvpSpinner)
        val eventsGroup = findViewById<LinearLayout>(R.id.eventsCheckboxGroup)
        val btnSave     = findViewById<Button>(R.id.btnSaveGuest)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val sides = listOf("Bride Side", "Groom Side", "Other")
        sideSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides)

        val rsvpOptions = listOf("Pending", "Confirmed", "Declined")
        rsvpSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rsvpOptions)

        // Load wedding events from Firestore to show as checkboxes
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val events = (doc.get("events") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            eventsGroup.removeAllViews()
            for (event in events) {
                val cb = CheckBox(this)
                cb.text = event
                cb.setTextColor(resources.getColor(R.color.rose_title, null))
                cb.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedEvents.add(event) else selectedEvents.remove(event)
                }
                eventsGroup.addView(cb)
            }
        }

        btnSave.setOnClickListener {
            val name  = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val side  = sideSpinner.selectedItem.toString()
            val rsvp  = rsvpSpinner.selectedItem.toString()

            if (name.isEmpty()) {
                nameInput.error = "Enter guest name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "SAVING..."

            val guestData = hashMapOf(
                "name"   to name,
                "phone"  to phone,
                "side"   to side,
                "rsvp"   to rsvp,
                "events" to selectedEvents.toList()
            )

            db.collection("users").document(uid).collection("guests")
                .add(guestData)
                .addOnSuccessListener {
                    Toast.makeText(this, "$name added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    btnSave.text = "SAVE GUEST"
                    Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
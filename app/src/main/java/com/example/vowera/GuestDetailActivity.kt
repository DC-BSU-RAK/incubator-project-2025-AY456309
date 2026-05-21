package com.example.vowera

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GuestDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var guestId = ""
    private val selectedEvents = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_detail)

        db      = FirebaseFirestore.getInstance()
        auth    = FirebaseAuth.getInstance()
        guestId = intent.getStringExtra("guestId") ?: ""

        val nameInput   = findViewById<EditText>(R.id.guestNameInput)
        val phoneInput  = findViewById<EditText>(R.id.guestPhoneInput)
        val sideSpinner = findViewById<Spinner>(R.id.guestSideSpinner)
        val rsvpSpinner = findViewById<Spinner>(R.id.guestRsvpSpinner)
        val eventsGroup = findViewById<LinearLayout>(R.id.eventsCheckboxGroup)
        val btnUpdate   = findViewById<Button>(R.id.btnUpdateGuest)
        val btnDelete   = findViewById<Button>(R.id.btnDeleteGuest)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val sides = listOf("Bride Side", "Groom Side", "Other")
        sideSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides)

        val rsvpOptions = listOf("Pending", "Confirmed", "Declined")
        rsvpSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rsvpOptions)

        val uid = auth.currentUser?.uid ?: return

        // Load all wedding events, then load guest data on top
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val allEvents = (userDoc.get("events") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            db.collection("users").document(uid).collection("guests").document(guestId)
                .get().addOnSuccessListener { guestDoc ->
                    val name        = guestDoc.getString("name") ?: ""
                    val phone       = guestDoc.getString("phone") ?: ""
                    val side        = guestDoc.getString("side") ?: "Bride Side"
                    val rsvp        = guestDoc.getString("rsvp") ?: "Pending"
                    val guestEvents = (guestDoc.get("events") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    nameInput.setText(name)
                    phoneInput.setText(phone)
                    sideSpinner.setSelection(sides.indexOf(side).coerceAtLeast(0))
                    rsvpSpinner.setSelection(rsvpOptions.indexOf(rsvp).coerceAtLeast(0))

                    selectedEvents.clear()
                    selectedEvents.addAll(guestEvents)

                    eventsGroup.removeAllViews()
                    for (event in allEvents) {
                        val cb = CheckBox(this)
                        cb.text = event
                        cb.isChecked = event in guestEvents
                        cb.setTextColor(resources.getColor(R.color.rose_title, null))
                        cb.setOnCheckedChangeListener { _, checked ->
                            if (checked) selectedEvents.add(event) else selectedEvents.remove(event)
                        }
                        eventsGroup.addView(cb)
                    }
                }
        }

        btnUpdate.setOnClickListener {
            val name  = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val side  = sideSpinner.selectedItem.toString()
            val rsvp  = rsvpSpinner.selectedItem.toString()

            if (name.isEmpty()) {
                nameInput.error = "Enter guest name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            btnUpdate.isEnabled = false
            btnUpdate.text = "SAVING..."

            val updatedData: Map<String, Any> = mapOf(
                "name"   to name,
                "phone"  to phone,
                "side"   to side,
                "rsvp"   to rsvp,
                "events" to selectedEvents.toList()
            )

            db.collection("users").document(uid).collection("guests").document(guestId)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Guest updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "UPDATE GUEST"
                    Toast.makeText(this, "Update failed. Try again.", Toast.LENGTH_SHORT).show()
                }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove Guest")
                .setMessage("Are you sure you want to remove this guest?")
                .setPositiveButton("Remove") { _, _ ->
                    db.collection("users").document(uid).collection("guests").document(guestId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Guest removed", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
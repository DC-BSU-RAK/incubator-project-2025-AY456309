package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GuestListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val allGuests = mutableListOf<Guest>()
    private val filteredGuests = mutableListOf<Guest>()
    private var activeFilter = "All"

    private lateinit var guestContainer: LinearLayout
    private lateinit var totalCountText: TextView
    private lateinit var confirmedCountText: TextView
    private lateinit var pendingCountText: TextView
    private lateinit var declinedCountText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_list)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        guestContainer     = findViewById(R.id.guestContainer)
        totalCountText     = findViewById(R.id.totalGuestCount)
        confirmedCountText = findViewById(R.id.confirmedCount)
        pendingCountText   = findViewById(R.id.pendingCount)
        declinedCountText  = findViewById(R.id.declinedCount)
        emptyText          = findViewById(R.id.emptyGuestText)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.filterAll).setOnClickListener   { setFilter("All") }
        findViewById<TextView>(R.id.filterBride).setOnClickListener { setFilter("Bride Side") }
        findViewById<TextView>(R.id.filterGroom).setOnClickListener { setFilter("Groom Side") }
        findViewById<TextView>(R.id.filterOther).setOnClickListener { setFilter("Other") }

        findViewById<FloatingActionButton>(R.id.fabAddGuest).setOnClickListener {
            startActivity(Intent(this, AddGuestActivity::class.java))
        }

        loadGuests()
    }

    override fun onResume() {
        super.onResume()
        loadGuests()
    }

    private fun loadGuests() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("guests")
            .get()
            .addOnSuccessListener { snapshot ->
                allGuests.clear()
                for (doc in snapshot.documents) {
                    allGuests.add(
                        Guest(
                            id     = doc.id,
                            name   = doc.getString("name") ?: "",
                            phone  = doc.getString("phone") ?: "",
                            side   = doc.getString("side") ?: "",
                            rsvp   = doc.getString("rsvp") ?: "Pending",
                            events = (doc.get("events") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        )
                    )
                }
                applyFilter()
                updateSummary()
            }
    }

    private fun setFilter(filter: String) {
        activeFilter = filter

        val btnAll   = findViewById<TextView>(R.id.filterAll)
        val btnBride = findViewById<TextView>(R.id.filterBride)
        val btnGroom = findViewById<TextView>(R.id.filterGroom)
        val btnOther = findViewById<TextView>(R.id.filterOther)

        listOf(btnAll, btnBride, btnGroom, btnOther).forEach {
            it.setBackgroundResource(R.drawable.input_bg)
            it.setTextColor(resources.getColor(R.color.rose_title, null))
        }

        val active = when (filter) {
            "Bride Side" -> btnBride
            "Groom Side" -> btnGroom
            "Other"      -> btnOther
            else         -> btnAll
        }
        active.setBackgroundResource(R.drawable.culture_card_selected)
        applyFilter()
    }

    private fun applyFilter() {
        filteredGuests.clear()
        filteredGuests.addAll(
            if (activeFilter == "All") allGuests
            else allGuests.filter { it.side == activeFilter }
        )
        renderGuests()
    }

    private fun renderGuests() {
        guestContainer.removeAllViews()

        if (filteredGuests.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }
        emptyText.visibility = View.GONE

        for (guest in filteredGuests) {
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_guest_row, guestContainer, false)

            row.findViewById<TextView>(R.id.guestName).text  = guest.name
            row.findViewById<TextView>(R.id.guestSide).text  = guest.side
            row.findViewById<TextView>(R.id.guestPhone).text = guest.phone.ifEmpty { "No phone" }

            val rsvpBadge = row.findViewById<TextView>(R.id.guestRsvp)
            rsvpBadge.text = guest.rsvp
            rsvpBadge.setBackgroundResource(
                when (guest.rsvp) {
                    "Confirmed" -> R.drawable.badge_confirmed
                    "Declined"  -> R.drawable.badge_declined
                    else        -> R.drawable.badge_pending
                }
            )

            row.setOnClickListener {
                val intent = Intent(this, GuestDetailActivity::class.java)
                intent.putExtra("guestId", guest.id)
                startActivity(intent)
            }

            guestContainer.addView(row)
        }
    }

    private fun updateSummary() {
        totalCountText.text     = "${allGuests.size}"
        confirmedCountText.text = "${allGuests.count { it.rsvp == "Confirmed" }}"
        pendingCountText.text   = "${allGuests.count { it.rsvp == "Pending" }}"
        declinedCountText.text  = "${allGuests.count { it.rsvp == "Declined" }}"
    }
}
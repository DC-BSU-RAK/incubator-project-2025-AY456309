package com.example.vowera

import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val eventName = intent.getStringExtra("eventName") ?: ""
        val currentUser = auth.currentUser

        if (currentUser == null || eventName.isEmpty()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupNavigation()
        loadEventDetails(currentUser.uid, eventName)

        // --- TabLayout & ViewPager2 setup ---
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = EventDetailsPagerAdapter(this, eventName, currentUser.uid)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Rituals"
                1 -> "Checklist"
                2 -> "Guests"
                else -> ""
            }
        }.attach()
    }

    private fun setupNavigation() {
        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

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

        btnBack.setOnClickListener {
            finish()
        }

        navDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        navEvents.setOnClickListener {
            // Already on Events
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
            Toast.makeText(this, "Budget screen next", Toast.LENGTH_SHORT).show()
        }

        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
        }
    }

    private fun loadEventDetails(uid: String, eventName: String) {
        val tvEventName = findViewById<TextView>(R.id.tvEventName)
        val tvEventDate = findViewById<TextView>(R.id.tvEventDate)
        val tvEventLocation = findViewById<TextView>(R.id.tvEventLocation)
        val tvEventBudget = findViewById<TextView>(R.id.tvEventBudget)

        val sanitizedName = sanitizeEventName(eventName)

        db.collection("users").document(uid)
            .collection("events").document(sanitizedName).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val event = doc.toObject(Event::class.java)
                    if (event != null) {
                        tvEventName.text = event.name
                        tvEventDate.text = event.date.ifEmpty { "Not set" }
                        tvEventLocation.text = event.location.ifEmpty { "Not set" }
                        tvEventBudget.text = getString(R.string.budget_format, event.budget)
                    }
                } else {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                finish()
            }
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

class EventDetailsPagerAdapter(activity: FragmentActivity, private val eventName: String, private val uid: String) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> RitualsFragment()
            1 -> ChecklistFragment()
            2 -> GuestsFragment()
            else -> RitualsFragment()
        }
        fragment.arguments = Bundle().apply {
            putString("eventName", eventName)
            putString("uid", uid)
        }
        return fragment
    }
}

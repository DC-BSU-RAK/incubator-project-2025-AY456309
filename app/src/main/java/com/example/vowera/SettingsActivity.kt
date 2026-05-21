package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Header buttons
        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)

        // Settings options
        val settingAccount = findViewById<LinearLayout>(R.id.settingAccount)
        val settingNotifications = findViewById<LinearLayout>(R.id.settingNotifications)
        val settingTheme = findViewById<LinearLayout>(R.id.settingTheme)
        val settingLanguage = findViewById<LinearLayout>(R.id.settingLanguage)
        val settingAbout = findViewById<LinearLayout>(R.id.settingAbout)
        val settingLogout = findViewById<LinearLayout>(R.id.settingLogout)

        // Bottom navigation
        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)

        // Set click listeners
        btnNotif.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }
        btnMore.setOnClickListener {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show()
        }

        settingAccount.setOnClickListener {
            Toast.makeText(this, "Account settings coming soon", Toast.LENGTH_SHORT).show()
        }
        settingNotifications.setOnClickListener {
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show()
        }
        settingTheme.setOnClickListener {
            Toast.makeText(this, "Theme settings coming soon", Toast.LENGTH_SHORT).show()
        }
        settingLanguage.setOnClickListener {
            Toast.makeText(this, "Language settings coming soon", Toast.LENGTH_SHORT).show()
        }
        settingAbout.setOnClickListener {
            Toast.makeText(this, "About screen coming soon", Toast.LENGTH_SHORT).show()
        }
        settingLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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
}
package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BudgetActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)

        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)

        btnNotif.setOnClickListener {
            Toast.makeText(this, getString(R.string.notifications_coming_soon), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For now, just navigate to TimelineActivity
            startActivity(Intent(this, TimelineActivity::class.java))
            finish()
        }

        navBudget.setOnClickListener {
            // Stay on current screen - do nothing
        }

        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
        }

        checkSessionAndLoad()
    }

    private fun checkSessionAndLoad() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadBudgetData(currentUser.uid)
    }

    private fun loadBudgetData(uid: String) {
        val tvTotalBudget = findViewById<TextView>(R.id.tvTotalBudget)
        val tvAllocatedBudget = findViewById<TextView>(R.id.tvAllocatedBudget)
        val tvBudgetPercentage = findViewById<TextView>(R.id.tvBudgetPercentage)
        val progressBudget = findViewById<ProgressBar>(R.id.progressBudget)
        val tvCurrency = findViewById<TextView>(R.id.tvCurrency)

        // Category breakdown views
        val tvVenueAmount = findViewById<TextView>(R.id.tvVenueAmount)
        val tvFoodAmount = findViewById<TextView>(R.id.tvFoodAmount)
        val tvDecorAmount = findViewById<TextView>(R.id.tvDecorAmount)
        val tvOutfitsAmount = findViewById<TextView>(R.id.tvOutfitsAmount)
        val tvPhotographyAmount = findViewById<TextView>(R.id.tvPhotographyAmount)
        val tvMakeupAmount = findViewById<TextView>(R.id.tvMakeupAmount)
        val tvTransportAmount = findViewById<TextView>(R.id.tvTransportAmount)

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, getString(R.string.no_user_data_found), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val totalBudget = doc.getString("totalBudget").orEmpty()
                val currency = doc.getString("currency").orEmpty()

                @Suppress("UNCHECKED_CAST")
                val categories = doc.get("categories") as? Map<String, Any> ?: emptyMap()

                val totalBudgetValue = totalBudget.toDoubleOrNull() ?: 0.0
                var allocated = 0.0

                // Calculate allocated amount
                categories.values.forEach { value ->
                    allocated += value.toString().toDoubleOrNull() ?: 0.0
                }

                val percent = if (totalBudgetValue > 0) {
                    ((allocated / totalBudgetValue) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                // Update total budget display
                tvTotalBudget.text = if (totalBudgetValue > 0) {
                    if (totalBudgetValue % 1.0 == 0.0) totalBudgetValue.toInt().toString() else totalBudgetValue.toString()
                } else {
                    getString(R.string.not_set)
                }

                // Update allocated budget display
                tvAllocatedBudget.text = if (allocated > 0) {
                    if (allocated % 1.0 == 0.0) allocated.toInt().toString() else allocated.toString()
                } else {
                    "0"
                }

                // Update percentage and progress
                tvBudgetPercentage.text = getString(R.string.percentage, percent)
                progressBudget.progress = percent

                // Update currency
                tvCurrency.text = currency.ifEmpty { getString(R.string.currency_not_set) }

                // Update category breakdowns
                tvVenueAmount.text = formatAmount(categories["venue"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvFoodAmount.text = formatAmount(categories["food"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvDecorAmount.text = formatAmount(categories["decor"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvOutfitsAmount.text = formatAmount(categories["outfits"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvPhotographyAmount.text = formatAmount(categories["photography"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvMakeupAmount.text = formatAmount(categories["makeup"]?.toString()?.toDoubleOrNull() ?: 0.0)
                tvTransportAmount.text = formatAmount(categories["transport"]?.toString()?.toDoubleOrNull() ?: 0.0)

            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_load_budget, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            amount.toString()
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

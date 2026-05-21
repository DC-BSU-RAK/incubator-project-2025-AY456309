package com.example.vowera

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class PremiumActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        auth = FirebaseAuth.getInstance()

        setupNavigation()
        setupPremiumButton()
    }

    private fun setupPremiumButton() {
        val btnGetPremium = findViewById<Button>(R.id.btnGetPremium)
        btnGetPremium.setOnClickListener {
            showPackageSelectionDialog()
        }
    }

    private fun showPackageSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_package, null)

        val layoutFree = dialogView.findViewById<LinearLayout>(R.id.layoutFreePackage)
        val layoutIndividual = dialogView.findViewById<LinearLayout>(R.id.layoutIndividualPackage)
        val layoutBundle = dialogView.findViewById<LinearLayout>(R.id.layoutBundlePackage)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        layoutFree.setOnClickListener {
            selectPackage("free", "Free Package", 0.0)
            dialog.dismiss()
        }

        layoutIndividual.setOnClickListener {
            selectPackage("individual", "Individual Package", 12.99)
            dialog.dismiss()
        }

        layoutBundle.setOnClickListener {
            selectPackage("bundle", "Premium Bundle", 25.0)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun selectPackage(packageId: String, packageName: String, price: Double) {
        if (packageId == "free") {
            Toast.makeText(this, "You are already on Free Plan", Toast.LENGTH_SHORT).show()
            return
        }

        // Navigate to payment checkout
        val intent = Intent(this, PaymentCheckoutActivity::class.java)
        intent.putExtra("packageId", packageId)
        intent.putExtra("packageName", packageName)
        intent.putExtra("packagePrice", price)
        startActivityForResult(intent, PAYMENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val success = data?.getBooleanExtra("success", false) ?: false
            if (success) {
                val packageName = data?.getStringExtra("packageName") ?: "Package"
                Toast.makeText(this, "Welcome to $packageName!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupNavigation() {
        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)

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
            startActivity(Intent(this, BudgetActivity::class.java))
            finish()
        }

        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
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
                    // Already on Premium screen
                    Toast.makeText(this, "Already on Premium screen", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val PAYMENT_REQUEST_CODE = 100
    }
}

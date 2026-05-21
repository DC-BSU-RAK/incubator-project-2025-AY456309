package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class PaymentCheckoutActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var selectedPaymentMethod = "card"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_checkout)

        auth = FirebaseAuth.getInstance()

        val packageId = intent.getStringExtra("packageId") ?: return
        val packageName = intent.getStringExtra("packageName") ?: "Package"
        val packagePrice = intent.getDoubleExtra("packagePrice", 0.0)

        // Setup UI
        val tvPackageName: TextView = findViewById(R.id.tvPackageName)
        val tvPackagePrice: TextView = findViewById(R.id.tvPackagePrice)
        val tvTotalAmount: TextView = findViewById(R.id.tvTotalAmount)
        val radioGroupPayment: RadioGroup = findViewById(R.id.radioGroupPayment)
        val btnConfirmPayment: Button = findViewById(R.id.btnConfirmPayment)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        // Set package details
        tvPackageName.text = packageName
        tvPackagePrice.text = String.format("$%.2f/month", packagePrice)
        tvTotalAmount.text = String.format("Total: $%.2f", packagePrice)

        // Handle payment method selection
        radioGroupPayment.setOnCheckedChangeListener { _, checkedId ->
            selectedPaymentMethod = when (checkedId) {
                R.id.radioCash -> "cash"
                R.id.radioCard -> "card"
                R.id.radioPayPal -> "paypal"
                else -> "card"
            }
        }

        // Confirm payment button
        btnConfirmPayment.setOnClickListener {
            processPayment(packageId, packageName, packagePrice, selectedPaymentMethod)
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun processPayment(packageId: String, packageName: String, price: Double, method: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val paymentMethodName = when (method) {
            "cash" -> "Cash Payment"
            "card" -> "Credit Card"
            "paypal" -> "PayPal"
            else -> "Card"
        }

        Toast.makeText(
            this,
            "Payment Successful!\nPackage: $packageName\nAmount: $$price\nMethod: $paymentMethodName",
            Toast.LENGTH_LONG
        ).show()

        // Simulate payment success - in real app, integrate with payment gateway
        val resultIntent = Intent()
        resultIntent.putExtra("packageId", packageId)
        resultIntent.putExtra("packageName", packageName)
        resultIntent.putExtra("price", price)
        resultIntent.putExtra("paymentMethod", paymentMethodName)
        resultIntent.putExtra("success", true)

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}


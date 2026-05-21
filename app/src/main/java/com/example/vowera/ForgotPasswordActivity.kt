package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val btnSendReset = findViewById<Button>(R.id.btnSendReset)
        val backToLoginText = findViewById<TextView>(R.id.backToLoginText)

        btnSendReset.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "Enter email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter a valid email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            btnSendReset.isEnabled = false
            btnSendReset.text = "SENDING..."

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    btnSendReset.isEnabled = true
                    btnSendReset.text = "SEND RESET LINK"

                    if (task.isSuccessful) {
                        showMessageDialog(
                            title = "Reset Link Sent",
                            message = "If this email is registered, a password reset link has been sent."
                        ) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    } else {
                        showMessageDialog(
                            title = "Reset Failed",
                            message = task.exception?.localizedMessage
                                ?: "Failed to send reset email."
                        )
                    }
                }
        }

        backToLoginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showMessageDialog(
        title: String,
        message: String,
        onOkClick: (() -> Unit)? = null
    ) {
        if (isFinishing || isDestroyed) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogIcon = dialogView.findViewById<ImageView>(R.id.dialogIcon)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val dialogButton = dialogView.findViewById<Button>(R.id.dialogButton)

        dialogTitle.text = title
        dialogMessage.text = message
        dialogButton.text = "OK"
        dialogIcon.setImageResource(R.drawable.vowera_logo)

        dialogButton.setOnClickListener {
            dialog.dismiss()
            onOkClick?.invoke()
        }

        dialog.show()
    }
}
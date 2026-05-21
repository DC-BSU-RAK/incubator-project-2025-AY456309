package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        val forgotPassword = findViewById<TextView>(R.id.forgotPasswordText)
        val signUpText = findViewById<TextView>(R.id.signUpText)
        val googleCard = findViewById<LinearLayout>(R.id.googleCard)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)
        val loginBtn = findViewById<Button>(R.id.btnLogin)

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

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

            if (password.isEmpty()) {
                passwordInput.error = "Enter password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            loginBtn.isEnabled = false
            loginBtn.text = "LOGGING IN..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    loginBtn.isEnabled = true
                    loginBtn.text = "LOG IN"

                    if (task.isSuccessful) {
                        showMessageDialog(
                            title = "Success",
                            message = "Login successful",
                            isSuccess = true
                        ) {
                            routeLoggedInUser()
                        }
                    } else {
                        when (task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                showMessageDialog(
                                    title = "Login Failed",
                                    message = "No account found. Please sign up first.",
                                    isSuccess = false
                                )
                            }

                            is FirebaseAuthInvalidCredentialsException -> {
                                showMessageDialog(
                                    title = "Login Failed",
                                    message = "Wrong email or password.",
                                    isSuccess = false
                                )
                            }

                            else -> {
                                showMessageDialog(
                                    title = "Login Failed",
                                    message = task.exception?.localizedMessage ?: "Login failed",
                                    isSuccess = false
                                )
                            }
                        }
                    }
                }
        }

        googleCard.setOnClickListener {
            startGoogleLogin()
        }

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                passwordInput.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_eye_open)
            } else {
                passwordInput.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_eye_closed)
            }

            passwordInput.setSelection(passwordInput.text.length)
        }
    }

    private fun routeLoggedInUser() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = currentUser.uid

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }

                val profileCompleted = doc.getBoolean("profileCompleted") ?: false
                val cultureCompleted = doc.getBoolean("cultureCompleted") ?: false
                val eventsCompleted = doc.getBoolean("eventsCompleted") ?: false
                val budgetCompleted = doc.getBoolean("budgetCompleted") ?: false
                val onboardingComplete = doc.getBoolean("onboardingComplete") ?: false

                val yourName = doc.getString("yourName").orEmpty()
                val partnerName = doc.getString("partnerName").orEmpty()
                val role = doc.getString("role").orEmpty()
                val culture = doc.getString("culture").orEmpty()

                @Suppress("UNCHECKED_CAST")
                val ceremonies = ArrayList(doc.get("suggestedCeremonies") as? List<String> ?: emptyList())

                @Suppress("UNCHECKED_CAST")
                val events = ArrayList(doc.get("events") as? List<String> ?: emptyList())

                val nextIntent = when {
                    !profileCompleted -> {
                        Intent(this, ProfileSetupActivity::class.java)
                    }

                    !cultureCompleted -> {
                        Intent(this, CultureSelectionActivity::class.java).apply {
                            putExtra("yourName", yourName)
                            putExtra("partnerName", partnerName)
                            putExtra("role", role)
                        }
                    }

                    !eventsCompleted -> {
                        Intent(this, EventSetupActivity::class.java).apply {
                            putExtra("culture", culture)
                            putExtra("yourName", yourName)
                            putExtra("partnerName", partnerName)
                            putExtra("role", role)
                            putStringArrayListExtra("ceremonies", ceremonies)
                        }
                    }

                    !budgetCompleted -> {
                        Intent(this, BudgetSetupActivity::class.java).apply {
                            putExtra("yourName", yourName)
                            putExtra("partnerName", partnerName)
                            putExtra("role", role)
                            putStringArrayListExtra("events", events)
                        }
                    }

                    onboardingComplete -> {
                        Intent(this, DashboardActivity::class.java)
                    }

                    else -> {
                        Intent(this, DashboardActivity::class.java)
                    }
                }

                nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(nextIntent)
                finish()
            }
            .addOnFailureListener {
                val intent = Intent(this, ProfileSetupActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun startGoogleLogin() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    showMessageDialog(
                        title = "Google Login Failed",
                        message = "Google login failed.",
                        isSuccess = false
                    )
                }
            } catch (e: GetCredentialException) {
                showMessageDialog(
                    title = "Google Login Cancelled",
                    message = e.message ?: "Google login cancelled",
                    isSuccess = false
                )
            } catch (e: GoogleIdTokenParsingException) {
                showMessageDialog(
                    title = "Google Login Failed",
                    message = "Google token parsing failed.",
                    isSuccess = false
                )
            } catch (e: Exception) {
                showMessageDialog(
                    title = "Google Login Failed",
                    message = e.message ?: "Google login failed",
                    isSuccess = false
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true

                    if (isNewUser) {
                        val user = auth.currentUser
                        user?.delete()?.addOnCompleteListener {
                            auth.signOut()
                            showMessageDialog(
                                title = "Login Failed",
                                message = "No Google account found for login. Please sign up first.",
                                isSuccess = false
                            )
                        }
                    } else {
                        showMessageDialog(
                            title = "Success",
                            message = "Google login successful",
                            isSuccess = true
                        ) {
                            routeLoggedInUser()
                        }
                    }
                } else {
                    showMessageDialog(
                        title = "Google Login Failed",
                        message = task.exception?.localizedMessage ?: "Google login failed",
                        isSuccess = false
                    )
                }
            }
    }

    private fun showMessageDialog(
        title: String,
        message: String,
        isSuccess: Boolean,
        onOkClick: (() -> Unit)? = null
    ) {
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
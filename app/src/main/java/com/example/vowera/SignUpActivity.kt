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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // BLOCK SIGNUP SCREEN IF USER IS ALREADY LOGGED IN
        if (auth.currentUser != null) {
            showAlreadySignedUpDialog(
                "You are already signed in. Please log in instead."
            )
            return
        }

        val loginText = findViewById<TextView>(R.id.loginText)
        val googleCard = findViewById<LinearLayout>(R.id.googleCard)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)

        val passwordToggle = findViewById<ImageView>(R.id.passwordToggle)
        val confirmPasswordToggle = findViewById<ImageView>(R.id.confirmPasswordToggle)
        val signUpBtn = findViewById<Button>(R.id.btnSignUp)

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePassword(passwordInput, passwordToggle, isPasswordVisible)
        }

        confirmPasswordToggle.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePassword(confirmPasswordInput, confirmPasswordToggle, isConfirmPasswordVisible)
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        signUpBtn.setOnClickListener {
            val fullName = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (fullName.isEmpty()) {
                nameInput.error = "Enter full name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailInput.error = "Enter email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter valid email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Enter password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.error = "Confirm password"
                confirmPasswordInput.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.requestFocus()
                return@setOnClickListener
            }

            signUpBtn.isEnabled = false
            signUpBtn.text = getString(R.string.creating)

            // CHECK FIRST IF EMAIL ALREADY EXISTS
            auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { methodTask ->
                    if (!methodTask.isSuccessful) {
                        signUpBtn.isEnabled = true
                        signUpBtn.text = getString(R.string.create_account)

                        showMessageDialog(
                            title = "Sign Up Failed",
                            message = methodTask.exception?.localizedMessage ?: "Could not verify email",
                            isSuccess = false
                        )
                        return@addOnCompleteListener
                    }

                    val methods = methodTask.result?.signInMethods ?: emptyList()

                    if (methods.isNotEmpty()) {
                        signUpBtn.isEnabled = true
                        signUpBtn.text = getString(R.string.create_account)

                        showAlreadySignedUpDialog(
                            "An account with this email already exists. Please log in."
                        )
                        return@addOnCompleteListener
                    }

                    // ONLY CREATE IF EMAIL DOES NOT EXIST
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            signUpBtn.isEnabled = true
                            signUpBtn.text = getString(R.string.create_account)

                            if (task.isSuccessful) {
                                val currentUser = auth.currentUser

                                currentUser?.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build()
                                )?.addOnCompleteListener {
                                    showMessageDialog(
                                        title = "Success",
                                        message = "Account created successfully",
                                        isSuccess = true
                                    ) {
                                        val intent = Intent(this, ProfileSetupActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            } else {
                                val exception = task.exception

                                if (exception is FirebaseAuthUserCollisionException) {
                                    showAlreadySignedUpDialog(
                                        "This user is already signed up. Please log in instead."
                                    )
                                } else {
                                    showMessageDialog(
                                        title = "Sign Up Failed",
                                        message = exception?.localizedMessage ?: "Sign up failed",
                                        isSuccess = false
                                    )
                                }
                            }
                        }
                }
        }

        googleCard.setOnClickListener {
            startGoogleSignUp()
        }
    }

    private fun startGoogleSignUp() {
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
                    context = this@SignUpActivity,
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
                        title = "Google Sign Up Failed",
                        message = "Google sign-up failed",
                        isSuccess = false
                    )
                }
            } catch (e: GetCredentialException) {
                showMessageDialog(
                    title = "Google Sign Up Cancelled",
                    message = e.message ?: "Google sign-up cancelled",
                    isSuccess = false
                )
            } catch (e: GoogleIdTokenParsingException) {
                showMessageDialog(
                    title = "Google Sign Up Failed",
                    message = "Google token parsing failed",
                    isSuccess = false
                )
            } catch (e: Exception) {
                showMessageDialog(
                    title = "Google Sign Up Failed",
                    message = e.message ?: "Google sign-up failed",
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
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser) {
                        showMessageDialog(
                            title = "Success",
                            message = "Google sign-up successful",
                            isSuccess = true
                        ) {
                            val intent = Intent(this, ProfileSetupActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        auth.signOut()
                        showAlreadySignedUpDialog(
                            "This Google account is already registered. Please log in."
                        )
                    }
                } else {
                    val exception = task.exception

                    if (exception is FirebaseAuthUserCollisionException) {
                        showAlreadySignedUpDialog(
                            "This user is already signed up. Please log in instead."
                        )
                    } else {
                        showMessageDialog(
                            title = "Google Sign Up Failed",
                            message = exception?.localizedMessage ?: "Firebase Google auth failed",
                            isSuccess = false
                        )
                    }
                }
            }
    }

    private fun showAlreadySignedUpDialog(message: String) {
        showMessageDialog(
            title = "Account Already Exists",
            message = message,
            isSuccess = false
        ) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun togglePassword(
        editText: EditText,
        toggleIcon: ImageView,
        isVisible: Boolean
    ) {
        if (isVisible) {
            editText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            toggleIcon.setImageResource(R.drawable.ic_eye_open)
        } else {
            editText.transformationMethod =
                PasswordTransformationMethod.getInstance()
            toggleIcon.setImageResource(R.drawable.ic_eye_closed)
        }
        editText.setSelection(editText.text.length)
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
package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        val logoImage = findViewById<ImageView>(R.id.welcomeLogo)
        val startBtn = findViewById<Button>(R.id.btnStartPlanning)
        val contentLayout = findViewById<LinearLayout>(R.id.welcomeContent)

        contentLayout.alpha = 0f
        contentLayout.translationY = 80f
        contentLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .start()

        animateLogo(logoImage)

        startBtn.setOnClickListener {
            val nextIntent = if (auth.currentUser == null) {
                Intent(this, LoginActivity::class.java)
            } else {
                Intent(this, ProfileSetupActivity::class.java)
            }
            startActivity(nextIntent)
            finish()
        }
    }

    private fun animateLogo(logoImage: ImageView) {
        logoImage.animate()
            .translationY(-8f)
            .setDuration(1800)
            .withEndAction {
                logoImage.animate()
                    .translationY(0f)
                    .setDuration(1800)
                    .withEndAction {
                        animateLogo(logoImage)
                    }
                    .start()
            }
            .start()
    }
}
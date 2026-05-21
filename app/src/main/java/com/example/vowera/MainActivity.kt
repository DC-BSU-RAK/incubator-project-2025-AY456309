package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contentLayout = findViewById<LinearLayout>(R.id.contentLayout)
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val getStartedBtn = findViewById<Button>(R.id.getStartedBtn)
        val loginText = findViewById<TextView>(R.id.loginText)

        contentLayout.alpha = 0f
        contentLayout.translationY = 100f

        contentLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1200)
            .start()

        logoImage.animate()
            .translationY(-10f)
            .setDuration(1600)
            .withEndAction {
                logoImage.animate()
                    .translationY(0f)
                    .setDuration(1600)
                    .withEndAction {
                        logoImage.animate()
                            .translationY(-10f)
                            .setDuration(1600)
                            .start()
                    }
                    .start()
            }
            .start()

        getStartedBtn.scaleX = 0.85f
        getStartedBtn.scaleY = 0.85f
        getStartedBtn.alpha = 0f

        getStartedBtn.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(900)
            .setStartDelay(300)
            .start()

        val pressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press)
        val releaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release)

        getStartedBtn.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(pressAnim)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.startAnimation(releaseAnim)
            }
            false
        }

        getStartedBtn.setOnClickListener {
            it.animate()
                .alpha(0.8f)
                .setDuration(100)
                .withEndAction {
                    it.alpha = 1f
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                .start()
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
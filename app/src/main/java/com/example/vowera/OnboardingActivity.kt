package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.view.View

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var indicatorsLayout: LinearLayout
    private lateinit var skipText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var onboardingItems: List<OnboardingItem>

    private val autoSlideRunnable = object : Runnable {
        override fun run() {
            if (::viewPager.isInitialized) {
                val nextItem = if (viewPager.currentItem < onboardingItems.size - 1) {
                    viewPager.currentItem + 1
                } else {
                    0
                }
                viewPager.currentItem = nextItem
                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        indicatorsLayout = findViewById(R.id.layoutIndicators)
        skipText = findViewById(R.id.skipText)

        onboardingItems = listOf(
            OnboardingItem(
                R.drawable.onboarding_1,
                "Welcome to Vowera",
                "A smart, culture-aware wedding planning app designed to manage every ritual and event beautifully from start to finish."
            ),
            OnboardingItem(
                R.drawable.onboarding_2,
                "Budgets per event",
                "Track separate budgets, spending, and allocations for every ceremony to stay in full financial control."
            ),
            OnboardingItem(
                R.drawable.onboarding_3,
                "Rituals, guests & vendors",
                "Manage ritual-based checklists, guest lists, vendors, shopping, and outfits for each event with ease."
            ),
            OnboardingItem(
                R.drawable.onboarding_4,
                "Timelines & reminders",
                "Stay on top of wedding schedules with automated reminders, real-time alerts, and a stress-free planning experience."
            )
        )

        viewPager.adapter = OnboardingAdapter(onboardingItems)

        setupIndicators(onboardingItems.size)
        setCurrentIndicator(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                btnNext.text = if (position == onboardingItems.lastIndex) {
                    "Get Started"
                } else {
                    "Next"
                }

                handler.removeCallbacks(autoSlideRunnable)
                handler.postDelayed(autoSlideRunnable, 3000)
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < onboardingItems.lastIndex) {
                viewPager.currentItem += 1
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        skipText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupIndicators(count: Int) {
        indicatorsLayout.removeAllViews()

        for (i in 0 until count) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(18, 18)
            params.setMargins(8, 0, 8, 0)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.dot_inactive)
            indicatorsLayout.addView(dot)
        }
    }

    private fun setCurrentIndicator(position: Int) {
        for (i in 0 until indicatorsLayout.childCount) {
            val dot = indicatorsLayout.getChildAt(i)
            if (i == position) {
                dot.setBackgroundResource(R.drawable.dot_active)
                dot.scaleX = 1.15f
                dot.scaleY = 1.15f
            } else {
                dot.setBackgroundResource(R.drawable.dot_inactive)
                dot.scaleX = 1f
                dot.scaleY = 1f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(autoSlideRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoSlideRunnable)
    }
}
package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CultureSelectionActivity : AppCompatActivity() {

    private val ceremoniesMap = mapOf(
        "South Asian" to listOf("Engagement", "Dholki / Mayun", "Mehndi", "Barat", "Nikkah / Wedding Ceremony", "Walima / Reception"),
        "Muslim" to listOf("Engagement", "Nikkah", "Wedding Ceremony", "Walima / Reception"),
        "Christian" to listOf("Engagement", "Bridal Shower", "Rehearsal Dinner", "Wedding Ceremony", "Reception"),
        "Hindu" to listOf("Roka / Engagement", "Mehendi", "Haldi", "Sangeet", "Wedding Ceremony", "Reception"),
        "Sikh" to listOf("Roka", "Chunni Ceremony", "Anand Karaj", "Reception"),
        "Custom" to listOf("Ceremony 1", "Reception")
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedCulture: String = ""
    private val cultureButtons = mutableMapOf<String, LinearLayout>()

    private lateinit var suggestedLabel: TextView
    private lateinit var suggestedList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_culture_selection)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val yourName = intent.getStringExtra("yourName") ?: ""
        val partnerName = intent.getStringExtra("partnerName") ?: ""
        val role = intent.getStringExtra("role") ?: ""

        val btnContinue = findViewById<Button>(R.id.btnCultureContinue)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        suggestedLabel = findViewById(R.id.suggestedCeremoniesLabel)
        suggestedList = findViewById(R.id.suggestedCeremoniesList)

        val cultures = listOf("South Asian", "Muslim", "Christian", "Hindu", "Sikh", "Custom")
        val cardIds = listOf(
            R.id.cardSouthAsian, R.id.cardMuslim, R.id.cardChristian,
            R.id.cardHindu, R.id.cardSikh, R.id.cardCustom
        )

        cultures.forEachIndexed { index, culture ->
            val card = findViewById<LinearLayout>(cardIds[index])
            cultureButtons[culture] = card
            card.setOnClickListener {
                selectCulture(culture)
            }
        }

        loadSavedCulture()

        btnContinue.setOnClickListener {
            if (selectedCulture.isEmpty()) {
                Toast.makeText(this, "Please select a culture", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            btnContinue.isEnabled = false
            btnContinue.text = "SAVING..."

            val ceremonies = ArrayList(ceremoniesMap[selectedCulture] ?: emptyList())

            db.collection("users").document(uid)
                .set(
                    hashMapOf(
                        "culture" to selectedCulture,
                        "suggestedCeremonies" to ceremonies,
                        "cultureCompleted" to true
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    val intent = Intent(this, EventSetupActivity::class.java)
                    intent.putExtra("culture", selectedCulture)
                    intent.putExtra("yourName", yourName)
                    intent.putExtra("partnerName", partnerName)
                    intent.putExtra("role", role)
                    intent.putStringArrayListExtra("ceremonies", ceremonies)
                    startActivity(intent)
                    finish()
                }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, ProfileSetupActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@CultureSelectionActivity, ProfileSetupActivity::class.java))
            finish()
        }
    }

    private fun loadSavedCulture() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val savedCulture = doc.getString("culture").orEmpty()
                if (savedCulture.isNotEmpty()) {
                    selectCulture(savedCulture)
                }
            }
    }

    private fun selectCulture(culture: String) {
        selectedCulture = culture

        cultureButtons.forEach { (_, card) ->
            card.setBackgroundResource(R.drawable.input_bg)
        }

        cultureButtons[culture]?.setBackgroundResource(R.drawable.culture_card_selected)

        val ceremonies = ceremoniesMap[culture] ?: emptyList()
        suggestedLabel.visibility = TextView.VISIBLE
        suggestedList.visibility = TextView.VISIBLE
        suggestedList.text = ceremonies.joinToString("\n") { "• $it" }
    }
}
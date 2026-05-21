package com.example.vowera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vowera.network.CurrencyRetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

class BudgetSetupActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var countryBudgetInput: AutoCompleteTextView
    private lateinit var currencyInput: AutoCompleteTextView
    private lateinit var totalBudgetInput: EditText
    private lateinit var btnContinue: Button
    private lateinit var btnBack: ImageView

    private var countryNames: List<String> = emptyList()
    private val countryCurrencyMap = mutableMapOf<String, List<String>>()
    private var currentCurrencyList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_setup)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val events = intent.getStringArrayListExtra("events") ?: arrayListOf()
        val yourName = intent.getStringExtra("yourName") ?: ""
        val partnerName = intent.getStringExtra("partnerName") ?: ""
        val role = intent.getStringExtra("role") ?: ""

        countryBudgetInput = findViewById(R.id.countryBudgetInput)
        currencyInput = findViewById(R.id.currencyInput)
        totalBudgetInput = findViewById(R.id.totalBudgetInput)
        btnContinue = findViewById(R.id.btnBudgetContinue)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            startActivity(Intent(this, EventSetupActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@BudgetSetupActivity, EventSetupActivity::class.java))
            finish()
        }

        setupDropdowns()
        fetchCountriesAndCurrencies()
        loadSavedBudget()

        btnContinue.setOnClickListener {
            saveBudgetAndGoToDashboard(events, yourName, partnerName, role)
        }
    }

    private fun loadSavedBudget() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val budgetCountry = doc.getString("budgetCountry").orEmpty()
                val currency = doc.getString("currency").orEmpty()
                val totalBudget = doc.getString("totalBudget").orEmpty()

                countryBudgetInput.setText(budgetCountry, false)
                currencyInput.setText(currency, false)
                totalBudgetInput.setText(totalBudget)

                findViewById<EditText>(R.id.budgetVenue).setText(doc.get("categories.venue")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetDecor).setText(doc.get("categories.decor")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetOutfits).setText(doc.get("categories.outfits")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetFood).setText(doc.get("categories.food")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetPhotography).setText(doc.get("categories.photography")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetMakeup).setText(doc.get("categories.makeup")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetTransport).setText(doc.get("categories.transport")?.toString().orEmpty())
                findViewById<EditText>(R.id.budgetMisc).setText(doc.get("categories.miscellaneous")?.toString().orEmpty())
            }
    }

    private fun saveBudgetAndGoToDashboard(
        events: ArrayList<String>,
        yourName: String,
        partnerName: String,
        role: String
    ) {
        val total = totalBudgetInput.text.toString().trim()
        val selectedCountry = countryBudgetInput.text.toString().trim()
        val selectedCurrency = currencyInput.text.toString().trim()

        if (selectedCountry.isEmpty()) { countryBudgetInput.error = "Select country"; return }
        if (selectedCurrency.isEmpty()) { currencyInput.error = "Select currency"; return }
        if (total.isEmpty()) { totalBudgetInput.error = "Enter your total budget"; return }

        btnContinue.isEnabled = false
        btnContinue.text = "SAVING..."

        val uid = auth.currentUser?.uid ?: return

        val categories = mapOf(
            "venue" to findViewById<EditText>(R.id.budgetVenue).text.toString().trim(),
            "decor" to findViewById<EditText>(R.id.budgetDecor).text.toString().trim(),
            "outfits" to findViewById<EditText>(R.id.budgetOutfits).text.toString().trim(),
            "food" to findViewById<EditText>(R.id.budgetFood).text.toString().trim(),
            "photography" to findViewById<EditText>(R.id.budgetPhotography).text.toString().trim(),
            "makeup" to findViewById<EditText>(R.id.budgetMakeup).text.toString().trim(),
            "transport" to findViewById<EditText>(R.id.budgetTransport).text.toString().trim(),
            "miscellaneous" to findViewById<EditText>(R.id.budgetMisc).text.toString().trim()
        )

        val budgetData = hashMapOf(
            "yourName" to yourName,
            "partnerName" to partnerName,
            "role" to role,
            "events" to events,
            "budgetCountry" to selectedCountry,
            "currency" to selectedCurrency,
            "totalBudget" to total,
            "categories" to categories,
            "budgetCompleted" to true,
            "onboardingComplete" to true
        )

        db.collection("users").document(uid)
            .set(budgetData, SetOptions.merge())
            .addOnSuccessListener {
                val intent = Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                btnContinue.isEnabled = true
                btnContinue.text = "FINISH SETUP"
            }
    }

    private fun setupDropdowns() {
        countryBudgetInput.setOnClickListener {
            if (countryNames.isNotEmpty()) countryBudgetInput.showDropDown()
        }

        currencyInput.setOnClickListener {
            if (currencyInput.isEnabled && currentCurrencyList.isNotEmpty()) currencyInput.showDropDown()
        }

        countryBudgetInput.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedCountry = parent.getItemAtPosition(position)?.toString()?.trim().orEmpty()
                if (selectedCountry.isEmpty()) return@OnItemClickListener

                countryBudgetInput.setText(selectedCountry, false)
                currencyInput.setText("", false)
                currencyInput.isEnabled = false

                val currencies = countryCurrencyMap[selectedCountry].orEmpty()
                currentCurrencyList = currencies

                currencyInput.setAdapter(
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        currentCurrencyList
                    )
                )
                currencyInput.isEnabled = currentCurrencyList.isNotEmpty()
            }

        currencyInput.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedCurrency = parent.getItemAtPosition(position)?.toString()?.trim().orEmpty()
                if (selectedCurrency.isNotEmpty()) currencyInput.setText(selectedCurrency, false)
            }
    }

    private fun fetchCountriesAndCurrencies() {
        lifecycleScope.launch {
            try {
                val response = CurrencyRetrofitClient.api.getCountriesWithCurrencies()
                if (!response.isSuccessful) return@launch

                val body = response.body().orEmpty()
                val tempMap = mutableMapOf<String, List<String>>()

                body.forEach { item ->
                    val countryName = item.name?.common?.trim().orEmpty()
                    if (countryName.isEmpty()) return@forEach

                    val currencies = item.currencies
                        ?.map { entry ->
                            val code = entry.key
                            val value = entry.value
                            val name = value.name?.trim().orEmpty()
                            val symbol = value.symbol?.trim().orEmpty()

                            when {
                                name.isNotEmpty() && symbol.isNotEmpty() -> "$code - $name ($symbol)"
                                name.isNotEmpty() -> "$code - $name"
                                else -> code
                            }
                        }
                        ?.distinct()
                        ?.sorted()
                        .orEmpty()

                    tempMap[countryName] = currencies
                }

                countryCurrencyMap.clear()
                countryCurrencyMap.putAll(tempMap)
                countryNames = countryCurrencyMap.keys.sorted()

                countryBudgetInput.setAdapter(
                    ArrayAdapter(
                        this@BudgetSetupActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        countryNames
                    )
                )
            } catch (e: Exception) {
                Log.e("BudgetSetup", "Currency API error", e)
            }
        }
    }
}
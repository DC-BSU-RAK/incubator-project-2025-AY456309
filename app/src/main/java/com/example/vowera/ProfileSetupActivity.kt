package com.example.vowera

import android.app.DatePickerDialog
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
import com.example.vowera.network.CitiesRequest
import com.example.vowera.network.RetrofitClient
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var yourNameInput: EditText
    private lateinit var partnerNameInput: EditText
    private lateinit var weddingDateInput: EditText
    private lateinit var roleInput: AutoCompleteTextView
    private lateinit var countryInput: AutoCompleteTextView
    private lateinit var cityInput: AutoCompleteTextView
    private lateinit var eventsInput: EditText
    private lateinit var btnContinue: Button
    private lateinit var btnBack: ImageView

    private var countryList: List<String> = emptyList()
    private var cityList: List<String> = emptyList()
    private val roleList = listOf("Bride", "Groom")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        yourNameInput = findViewById(R.id.yourNameInput)
        partnerNameInput = findViewById(R.id.partnerNameInput)
        weddingDateInput = findViewById(R.id.weddingDateInput)
        roleInput = findViewById(R.id.roleInput)
        countryInput = findViewById(R.id.countryInput)
        cityInput = findViewById(R.id.cityInput)
        eventsInput = findViewById(R.id.eventsInput)
        btnContinue = findViewById(R.id.btnProfileContinue)
        btnBack = findViewById(R.id.btnBack)

        setupRoleDropdown()
        setupDatePicker()
        setupDropdownClicks()
        fetchCountries()
        loadSavedProfile()

        btnContinue.setOnClickListener {
            saveProfile()
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@ProfileSetupActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadSavedProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                yourNameInput.setText(doc.getString("yourName").orEmpty())
                partnerNameInput.setText(doc.getString("partnerName").orEmpty())
                weddingDateInput.setText(doc.getString("weddingDate").orEmpty())
                roleInput.setText(doc.getString("role").orEmpty(), false)

                val savedCountry = doc.getString("country").orEmpty()
                val savedCity = doc.getString("city").orEmpty()
                val expectedEvents = doc.getString("expectedEvents").orEmpty()

                countryInput.setText(savedCountry, false)
                eventsInput.setText(expectedEvents)

                if (savedCountry.isNotEmpty()) {
                    fetchCities(savedCountry, savedCity)
                }
            }
    }

    private fun setupRoleDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roleList)
        roleInput.setAdapter(adapter)

        roleInput.setOnClickListener { roleInput.showDropDown() }
        roleInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedRole = parent.getItemAtPosition(position)?.toString()?.trim().orEmpty()
            if (selectedRole.isNotEmpty()) roleInput.setText(selectedRole, false)
        }
    }

    private fun setupDatePicker() {
        weddingDateInput.isFocusable = false
        weddingDateInput.isClickable = true

        weddingDateInput.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    weddingDateInput.setText("$day/${month + 1}/$year")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupDropdownClicks() {
        countryInput.setOnClickListener {
            if (countryList.isNotEmpty()) countryInput.showDropDown()
        }

        cityInput.setOnClickListener {
            if (cityInput.isEnabled && cityList.isNotEmpty()) cityInput.showDropDown()
        }

        countryInput.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedCountry = parent.getItemAtPosition(position)?.toString()?.trim().orEmpty()

                if (selectedCountry.isEmpty()) {
                    Toast.makeText(this, "Invalid country selected", Toast.LENGTH_SHORT).show()
                    return@OnItemClickListener
                }

                countryInput.setText(selectedCountry, false)
                cityInput.setText("", false)
                cityInput.isEnabled = false
                cityList = emptyList()

                fetchCities(selectedCountry)
            }

        cityInput.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selectedCity = parent.getItemAtPosition(position)?.toString()?.trim().orEmpty()
                if (selectedCity.isNotEmpty()) cityInput.setText(selectedCity, false)
            }
    }

    private fun fetchCountries() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getCountries()
                if (!response.isSuccessful) return@launch

                val countries = response.body()?.data
                    ?.mapNotNull { it.country ?: it.name }
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()

                countryList = countries
                countryInput.setAdapter(
                    ArrayAdapter(
                        this@ProfileSetupActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        countryList
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchCities(country: String, preselectedCity: String = "") {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getCitiesByCountry(CitiesRequest(country = country))
                if (!response.isSuccessful) {
                    cityInput.isEnabled = false
                    return@launch
                }

                cityList = response.body()?.data
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()

                cityInput.setAdapter(
                    ArrayAdapter(
                        this@ProfileSetupActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        cityList
                    )
                )
                cityInput.isEnabled = true

                if (preselectedCity.isNotEmpty()) {
                    cityInput.setText(preselectedCity, false)
                }
            } catch (_: Exception) {
                cityInput.isEnabled = false
            }
        }
    }

    private fun saveProfile() {
        val yourName = yourNameInput.text.toString().trim()
        val partnerName = partnerNameInput.text.toString().trim()
        val weddingDate = weddingDateInput.text.toString().trim()
        val role = roleInput.text.toString().trim()
        val country = countryInput.text.toString().trim()
        val city = cityInput.text.toString().trim()
        val events = eventsInput.text.toString().trim()

        if (yourName.isEmpty()) { yourNameInput.error = "Enter your name"; return }
        if (partnerName.isEmpty()) { partnerNameInput.error = "Enter partner's name"; return }
        if (weddingDate.isEmpty()) { weddingDateInput.error = "Select wedding date"; return }
        if (role.isEmpty()) { roleInput.error = "Select role"; return }
        if (country.isEmpty()) { countryInput.error = "Select country"; return }
        if (city.isEmpty()) { cityInput.error = "Select city"; return }
        if (events.isEmpty()) { eventsInput.error = "Enter number of events"; return }

        val currentUser = auth.currentUser ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        btnContinue.isEnabled = false
        btnContinue.text = "SAVING..."

        val provider = currentUser.providerData.mapNotNull { it.providerId }
            .firstOrNull { it != "firebase" } ?: "email"

        val profileData = hashMapOf(
            "uid" to currentUser.uid,
            "email" to (currentUser.email ?: ""),
            "fullName" to (currentUser.displayName ?: ""),
            "yourName" to yourName,
            "partnerName" to partnerName,
            "weddingDate" to weddingDate,
            "role" to role,
            "country" to country,
            "city" to city,
            "expectedEvents" to events,
            "provider" to provider,
            "profileCompleted" to true,
            "updatedAt" to Timestamp.now()
        )

        db.collection("users").document(currentUser.uid)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                val intent = Intent(this, CultureSelectionActivity::class.java)
                intent.putExtra("yourName", yourName)
                intent.putExtra("partnerName", partnerName)
                intent.putExtra("role", role)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                btnContinue.isEnabled = true
                btnContinue.text = "CONTINUE"
            }
    }
}
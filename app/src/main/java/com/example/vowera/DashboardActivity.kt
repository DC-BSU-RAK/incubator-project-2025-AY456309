package com.example.vowera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imgRole: ImageView
    private lateinit var btnSetProfile: Button

    private val cloudName = "dshlfuqkq"
    private val uploadPreset = "vowera_unsigned"

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                uploadImageToCloudinary(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnNotif = findViewById<ImageView>(R.id.btnNotif)
        val btnMore = findViewById<ImageView>(R.id.btnMore)
        btnSetProfile = findViewById(R.id.btnSetProfile)
        imgRole = findViewById(R.id.imgRole)

        val navDashboard = findViewById<LinearLayout>(R.id.navDashboard)
        val navEvents = findViewById<LinearLayout>(R.id.navEvents)
        val navTimeline = findViewById<LinearLayout>(R.id.navTimeline)
        val navBudget = findViewById<LinearLayout>(R.id.navBudget)
        val navGuests = findViewById<LinearLayout>(R.id.navGuests)

        btnNotif.setOnClickListener {
            Toast.makeText(this, getString(R.string.notifications_coming_soon), Toast.LENGTH_SHORT).show()
        }

        btnMore.setOnClickListener {
            showMoreMenu(it)
        }

        btnSetProfile.setOnClickListener {
            openImagePicker()
        }

        imgRole.setOnClickListener {
            openImagePicker()
        }

        navDashboard.setOnClickListener {
            Toast.makeText(this, getString(R.string.dashboard), Toast.LENGTH_SHORT).show()
        }

        navEvents.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        navTimeline.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For now, just navigate to TimelineActivity
            startActivity(Intent(this, TimelineActivity::class.java))
            finish()
        }

        navBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        navGuests.setOnClickListener {
            startActivity(Intent(this, GuestListActivity::class.java))
            finish()
        }

        val cardShoppingList = findViewById<LinearLayout>(R.id.cardShoppingList)
        cardShoppingList.setOnClickListener {
            startActivity(Intent(this, ShoppingListActivity::class.java))
            finish()
        }

        val cardVendors = findViewById<LinearLayout>(R.id.cardVendors)
        cardVendors.setOnClickListener {
            startActivity(Intent(this, VendorActivity::class.java))
            finish()
        }

        val cardweddingchecklist = findViewById<LinearLayout>(R.id.cardweddingchecklist)
        cardweddingchecklist.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, WeddingChecklistActivity::class.java)
            intent.putExtra("eventName", "Wedding")
            startActivity(intent)
        }

        checkSessionAndLoad()
    }

    private fun openImagePicker() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_LONG).show()
            return
        }
        imagePickerLauncher.launch("image/*")
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_LONG).show()
            return
        }

        btnSetProfile.isEnabled = false
        btnSetProfile.text = getString(R.string.uploading)

        val oldDrawable = imgRole.drawable

        Glide.with(this)
            .load(uri)
            .apply(RequestOptions().circleCrop())
            .into(imgRole)

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    runOnUiThread {
                        btnSetProfile.isEnabled = true
                        btnSetProfile.text = getString(R.string.change_photo)
                        imgRole.setImageDrawable(oldDrawable)
                        Toast.makeText(this, getString(R.string.failed_to_read_image), Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val tempFile = File(cacheDir, "dashboard_profile_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart(
                        "file",
                        tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                    .post(requestBody)
                    .build()

                OkHttpClient().newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            btnSetProfile.isEnabled = true
                            btnSetProfile.text = getString(R.string.change_photo)
                            imgRole.setImageDrawable(oldDrawable)
                            Toast.makeText(
                                this@DashboardActivity,
                                getString(R.string.upload_failed, e.localizedMessage),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string().orEmpty()

                        if (!response.isSuccessful) {
                            runOnUiThread {
                                btnSetProfile.isEnabled = true
                                btnSetProfile.text = getString(R.string.change_photo)
                                imgRole.setImageDrawable(oldDrawable)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    getString(R.string.cloudinary_error, responseBody),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return
                        }

                        val secureUrl = Regex("\"secure_url\":\"(.*?)\"")
                            .find(responseBody)
                            ?.groupValues
                            ?.getOrNull(1)
                            ?.replace("\\/", "/")
                            .orEmpty()

                        if (secureUrl.isEmpty()) {
                            runOnUiThread {
                                btnSetProfile.isEnabled = true
                                btnSetProfile.text = getString(R.string.change_photo)
                                imgRole.setImageDrawable(oldDrawable)
                                Toast.makeText(
                                    this@DashboardActivity,
                                    getString(R.string.image_url_not_found),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return
                        }

                        saveImageUrlToFirestore(currentUser.uid, secureUrl)
                    }
                })
            } catch (e: Exception) {
                runOnUiThread {
                    btnSetProfile.isEnabled = true
                    btnSetProfile.text = getString(R.string.change_photo)
                    imgRole.setImageDrawable(oldDrawable)
                    Toast.makeText(
                        this,
                        getString(R.string.upload_failed, e.localizedMessage),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun saveImageUrlToFirestore(uid: String, imageUrl: String) {
        db.collection("users")
            .document(uid)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                btnSetProfile.isEnabled = true
                btnSetProfile.text = getString(R.string.change_photo)

                Glide.with(this)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                    )
                    .into(imgRole)

                Toast.makeText(this, getString(R.string.profile_image_updated), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                btnSetProfile.isEnabled = true
                btnSetProfile.text = getString(R.string.change_photo)
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_save_image, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun checkSessionAndLoad() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.please_login_again), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadDashboardData(currentUser.uid)
    }

    private fun loadDashboardData(uid: String) {
        val tvHelloLine = findViewById<TextView>(R.id.tvHelloLine)
        val tvHeroSub = findViewById<TextView>(R.id.tvHeroSub)

        val tvUpcomingTitle = findViewById<TextView>(R.id.tvUpcomingTitle)
        val tvUpcomingDate = findViewById<TextView>(R.id.tvUpcomingDate)
        val tvUpcomingTime = findViewById<TextView>(R.id.tvUpcomingTime)
        val tvUpcomingVendor = findViewById<TextView>(R.id.tvUpcomingVendor)

        val tvDaysLeft = findViewById<TextView>(R.id.tvDaysLeft)
        val tvWeddingDate = findViewById<TextView>(R.id.tvWeddingDate)

        val tvBudgetPercent = findViewById<TextView>(R.id.tvBudgetPercent)
        val tvBudgetLine = findViewById<TextView>(R.id.tvBudgetLine)

        val tvTaskOneTitle = findViewById<TextView>(R.id.tvTaskOneTitle)
        val tvTaskOneStatus = findViewById<TextView>(R.id.tvTaskOneStatus)
        val tvTaskTwoTitle = findViewById<TextView>(R.id.tvTaskTwoTitle)
        val tvTaskTwoStatus = findViewById<TextView>(R.id.tvTaskTwoStatus)

        val tvTimelineProgress = findViewById<TextView>(R.id.tvTimelineProgress)
        val tvTimelineSubtitle = findViewById<TextView>(R.id.tvTimelineSubtitle)
        val progressTimeline = findViewById<ProgressBar>(R.id.progressTimeline)
        val tvTimelineStart = findViewById<TextView>(R.id.tvTimelineStart)
        val tvTimelineToday = findViewById<TextView>(R.id.tvTimelineToday)
        val tvTimelineEnd = findViewById<TextView>(R.id.tvTimelineEnd)
        val layoutTimelineDetails = findViewById<LinearLayout>(R.id.layoutTimelineDetails)

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, getString(R.string.no_user_data_found), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val yourName = doc.getString("yourName").orEmpty()
                val partnerName = doc.getString("partnerName").orEmpty()
                val weddingDate = doc.getString("weddingDate").orEmpty()
                val role = doc.getString("role").orEmpty()
                val culture = doc.getString("culture").orEmpty()
                val city = doc.getString("city").orEmpty()
                val country = doc.getString("country").orEmpty()
                val totalBudget = doc.getString("totalBudget").orEmpty()
                val currency = doc.getString("currency").orEmpty()
                val profileImageUrl = doc.getString("profileImageUrl").orEmpty()
                val authPhotoUrl = auth.currentUser?.photoUrl?.toString().orEmpty()
                val expectedEventsText = doc.getString("expectedEvents").orEmpty()
                val timelineCompleted = doc.getBoolean("timelineCompleted") ?: false

                @Suppress("UNCHECKED_CAST")
                val events = doc.get("events") as? List<String> ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val categories = doc.get("categories") as? Map<String, Any> ?: emptyMap()

                tvHelloLine.text = if (yourName.isNotEmpty()) {
                    getString(R.string.welcome_with_name, yourName)
                } else {
                    getString(R.string.welcome)
                }

                val subtitle = buildString {
                    if (role.isNotEmpty()) append(role)

                    if (partnerName.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append("with $partnerName")
                    }

                    if (culture.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(culture)
                    }

                    val location = listOf(city, country)
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")

                    if (location.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(location)
                    }
                }

                tvHeroSub.text = if (subtitle.isNotEmpty()) {
                    subtitle
                } else {
                    getString(R.string.track_wedding_plans)
                }

                val finalImageUrl = when {
                    profileImageUrl.isNotEmpty() -> profileImageUrl
                    authPhotoUrl.isNotEmpty() -> authPhotoUrl
                    else -> ""
                }

                if (finalImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(finalImageUrl)
                        .apply(
                            RequestOptions()
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                        )
                        .into(imgRole)
                } else {
                    imgRole.setImageResource(R.drawable.ic_profile_placeholder)
                }

                val nextEvent = events.firstOrNull().orEmpty()

                tvUpcomingTitle.text = when {
                    nextEvent.isNotEmpty() -> nextEvent
                    expectedEventsText.isNotEmpty() -> "$expectedEventsText Events Planned"
                    else -> getString(R.string.no_upcoming_event)
                }

                // Make upcoming event card clickable if there's an event
                if (nextEvent.isNotEmpty()) {
                    val upcomingCard = tvUpcomingTitle.parent
                    if (upcomingCard is View) {
                        upcomingCard.setOnClickListener {
                            val intent = Intent(this@DashboardActivity, EventDetailActivity::class.java)
                            intent.putExtra("eventName", nextEvent)
                            this@DashboardActivity.startActivity(intent)
                        }
                    }
                }

                tvUpcomingDate.text = if (weddingDate.isNotEmpty()) weddingDate else getString(R.string.date_not_set)
                tvUpcomingVendor.text = if (city.isNotEmpty()) city else getString(R.string.vendor_not_assigned)

                tvWeddingDate.text = if (weddingDate.isNotEmpty()) weddingDate else getString(R.string.not_set)
                tvDaysLeft.text = calculateDaysLeft(weddingDate).toString()

                if (!timelineCompleted) {
                    layoutTimelineDetails.visibility = View.GONE
                    tvTimelineProgress.text = "0%"
                    tvTimelineSubtitle.text = getString(R.string.timeline_not_done_yet)
                    progressTimeline.progress = 0
                    tvTimelineStart.text = getString(R.string.not_set)
                    tvTimelineToday.text = getString(R.string.not_started)
                    tvTimelineEnd.text = getString(R.string.not_set)
                } else {
                    layoutTimelineDetails.visibility = View.VISIBLE
                    tvTimelineStart.text = getString(R.string.today)
                    tvTimelineEnd.text = if (weddingDate.isNotEmpty()) weddingDate else getString(R.string.not_set)
                    tvTimelineToday.text = getString(R.string.in_progress)

                    val timelinePercent = calculateTimelineProgress(weddingDate)
                    progressTimeline.progress = timelinePercent
                    tvTimelineProgress.text = "$timelinePercent%"

                    tvTimelineSubtitle.text = when {
                        weddingDate.isEmpty() -> getString(R.string.add_wedding_date_timeline)
                        timelinePercent >= 100 -> getString(R.string.big_day_here)
                        timelinePercent > 0 -> getString(R.string.timeline_percent_completed, timelinePercent)
                        else -> getString(R.string.planning_journey_started)
                    }
                }

                var allocated = 0.0
                categories.values.forEach { value ->
                    allocated += value.toString().toDoubleOrNull() ?: 0.0
                }

                val totalBudgetValue = totalBudget.toDoubleOrNull() ?: 0.0
                val percent = if (totalBudgetValue > 0) {
                    ((allocated / totalBudgetValue) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                tvBudgetPercent.text = "$percent%"

                tvBudgetLine.text = if (totalBudgetValue > 0) {
                    val allocatedText =
                        if (allocated % 1.0 == 0.0) allocated.toInt().toString() else allocated.toString()

                    val totalText =
                        if (totalBudgetValue % 1.0 == 0.0) totalBudgetValue.toInt().toString() else totalBudgetValue.toString()

                    if (currency.isNotEmpty()) {
                        "$allocatedText of $totalText $currency"
                    } else {
                        "$allocatedText of $totalText"
                    }
                } else {
                    getString(R.string.no_budget_set)
                }

                val taskTitles = mutableListOf<String>()
                if ((categories["food"]?.toString()?.toDoubleOrNull() ?: 0.0) > 0.0) {
                    taskTitles.add(getString(R.string.food_menu))
                }
                if ((categories["venue"]?.toString()?.toDoubleOrNull() ?: 0.0) > 0.0) {
                    taskTitles.add(getString(R.string.venue))
                }
                if ((categories["decor"]?.toString()?.toDoubleOrNull() ?: 0.0) > 0.0) {
                    taskTitles.add(getString(R.string.decor))
                }
                if (events.isNotEmpty() || expectedEventsText.isNotEmpty()) {
                    taskTitles.add(getString(R.string.guest_list))
                }

                tvTaskOneTitle.text = taskTitles.getOrNull(0) ?: getString(R.string.food_menu)
                tvTaskTwoTitle.text = taskTitles.getOrNull(1) ?: getString(R.string.guest_list)

                tvTaskOneStatus.text = if (taskTitles.isNotEmpty()) getString(R.string.pending) else getString(R.string.not_started_cap)
                tvTaskTwoStatus.text = if (events.isNotEmpty() || expectedEventsText.isNotEmpty()) {
                    getString(R.string.in_progress)
                } else {
                    getString(R.string.pending)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_load_dashboard, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun calculateDaysLeft(dateString: String): Int {
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val weddingDate = sdf.parse(dateString) ?: return 0
            val diff = weddingDate.time - System.currentTimeMillis()
            (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    private fun calculateTimelineProgress(dateString: String): Int {
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val weddingDate = sdf.parse(dateString) ?: return 0

            val now = System.currentTimeMillis()
            val totalDuration = weddingDate.time - now

            if (totalDuration <= 0L) return 100

            val sixMonthsMillis = TimeUnit.DAYS.toMillis(180)
            val elapsed = sixMonthsMillis - totalDuration

            ((elapsed.toDouble() / sixMonthsMillis.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }

    private fun showMoreMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menu.add(0, 1, 0, "Settings")
        popupMenu.menu.add(0, 2, 1, "Unlock the full version")
        popupMenu.menu.add(0, 3, 2, "Logout")

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                2 -> {
                    startActivity(Intent(this, PremiumActivity::class.java))
                    true
                }

                3 -> {
                    auth.signOut()
                    finish()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }
}
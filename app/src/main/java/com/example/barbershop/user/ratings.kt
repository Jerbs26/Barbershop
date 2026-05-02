package com.example.barbershop.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.util.Database
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.UserSidebarHelper

// Ratings screen — shows all ratings the logged-in user has previously submitted
class Ratings : AppCompatActivity() {

    private lateinit var btnNewBooking: Button
    private lateinit var btnBarbers: Button
    private lateinit var btnServices: Button
    private lateinit var btnProducts: Button
    private lateinit var btnHistory: Button
    private lateinit var btnRatings: Button
    private lateinit var btnLogout: Button
    private lateinit var ratingsContainer: LinearLayout
    private val hamburger = UserSidebarHelper()
    private var loggedInEmail: String = ""

    // Theme-aware color helpers so the UI looks right in both dark and light mode
    private val isDark      get() = DarkModeHelper.isDarkMode(this)
    private val cardBg      get() = if (isDark) Color.parseColor("#2C2C2C") else Color.WHITE
    private val textPrimary get() = if (isDark) Color.WHITE else Color.parseColor("#2C2C2C")
    private val textSecond  get() = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")
    private val textComment get() = if (isDark) Color.parseColor("#CCCCCC") else Color.parseColor("#444444")

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ratings)

        loggedInEmail = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("USER_EMAIL", "") ?: ""

        hamburger.setup(
            context     = this,
            sidebar     = findViewById(R.id.sidebar),
            contentArea = findViewById(R.id.contentArea)
        )

        btnNewBooking    = findViewById(R.id.btnNewBooking)
        btnBarbers       = findViewById(R.id.btnBarbers)
        btnServices      = findViewById(R.id.btnServices)
        btnProducts      = findViewById(R.id.btnProducts)
        btnHistory       = findViewById(R.id.btnHistory)
        btnRatings       = findViewById(R.id.btnRatings)
        btnLogout        = findViewById(R.id.btnLogout)
        ratingsContainer = findViewById(R.id.ratingsContainer)

        loadUserSidebar()
        setupNavigation()
        loadRatings()
    }

    override fun onResume() {
        super.onResume()
        loadRatings()
    }

    // Pull ratings from the database and build a card for each one
    private fun loadRatings() {
        ratingsContainer.removeAllViews()
        val db      = Database(this)
        val ratings = db.getRatingsByUser(loggedInEmail)

        if (ratings.isEmpty()) {
            ratingsContainer.addView(TextView(this).apply {
                text     = "You haven't submitted any ratings yet."
                textSize = 14f
                setTextColor(textSecond)
                gravity  = Gravity.CENTER
                setPadding(0, 64, 0, 0)
            })
            return
        }

        for (rating in ratings) {
            val barber  = rating["barber"]  ?: ""
            val service = rating["service"] ?: ""
            val date    = rating["date"]    ?: ""
            val score   = (rating["score"] ?: "0").toIntOrNull() ?: 0
            val comment = rating["comment"] ?: ""

            // Build the star string — filled stars up to the score, empty after
            val stars = "★".repeat(score) + "☆".repeat(5 - score)

            val card = CardView(this).apply {
                radius = dpToPx(8).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setCardBackgroundColor(cardBg)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(16) }
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }

            // Product orders get a different label than barber bookings
            if (barber == "Product Order") {
                inner.addView(TextView(this).apply {
                    text     = "🛍️ Product Order"
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(textPrimary)
                    layoutParams = lp(bottom = 4)
                })
            } else if (barber.isNotBlank()) {
                inner.addView(TextView(this).apply {
                    text     = "Barber: $barber"
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(textPrimary)
                    layoutParams = lp(bottom = 4)
                })
            }

            if (service.isNotBlank()) {
                inner.addView(TextView(this).apply {
                    text = "Service: $service"
                    textSize = 12f
                    setTextColor(textSecond)
                    layoutParams = lp(bottom = 2)
                })
            }

            if (date.isNotBlank()) {
                inner.addView(TextView(this).apply {
                    text = "Date: $date"
                    textSize = 12f
                    setTextColor(textSecond)
                    layoutParams = lp(bottom = 8)
                })
            }

            // Star display in gold
            inner.addView(TextView(this).apply {
                text = stars
                textSize = 18f
                setTextColor(Color.parseColor("#FFD700"))
                layoutParams = lp(bottom = 8)
            })

            // Only show the comment block if they actually wrote something
            if (comment.isNotBlank()) {
                inner.addView(TextView(this).apply {
                    text = "\"$comment\""
                    textSize = 13f
                    setTextColor(textComment)
                    setTypeface(null, Typeface.ITALIC)
                    layoutParams = lp(bottom = 4)
                })
            }

            card.addView(inner)
            ratingsContainer.addView(card)
        }
    }

    // Small helper to keep LayoutParams creation readable inline
    private fun lp(bottom: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).also { it.bottomMargin = dpToPx(bottom) }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Show the user's initials and email in the sidebar header
    private fun loadUserSidebar() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = email.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase() else namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        findViewById<TextView>(R.id.userEmail).text = email
    }

    // Confirm before logging out and clearing the session
    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        dialog.show()
    }

    // Sidebar navigation — close the drawer then navigate to the chosen screen
    private fun setupNavigation() {
        btnNewBooking.setOnClickListener { hamburger.closeSidebar(); startActivity(Intent(this, Dashboard::class.java)) }
        btnBarbers.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Barbers::class.java)) }
        btnServices.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Services::class.java)) }
        btnProducts.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Products::class.java)) }
        btnHistory.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, History::class.java)) }
        btnRatings.setOnClickListener    { hamburger.closeSidebar() } // already here, do nothing
        btnLogout.setOnClickListener     { showLogoutDialog() }
    }
}
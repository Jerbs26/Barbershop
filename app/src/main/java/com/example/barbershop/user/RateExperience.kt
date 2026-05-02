package com.example.barbershop.user

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.R
import com.example.barbershop.util.Database
import com.example.barbershop.util.DarkModeHelper

class RateExperience : AppCompatActivity() {

    private var selectedStars = 0
    // Keeping all five star TextViews in a list makes it easy to update them together
    private val starButtons   = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_experience)

        val isDark = DarkModeHelper.isDarkMode(this)

        // Find out who/what we're rating
        val email   = intent.getStringExtra("email")   ?: ""
        val barber  = intent.getStringExtra("barber")  ?: ""
        val service = intent.getStringExtra("service") ?: ""
        val type    = intent.getStringExtra("type")    ?: "booking"

        val tvTitle    = findViewById<TextView>(R.id.tvRateTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvRateSubtitle)
        val star1      = findViewById<TextView>(R.id.star1)
        val star2      = findViewById<TextView>(R.id.star2)
        val star3      = findViewById<TextView>(R.id.star3)
        val star4      = findViewById<TextView>(R.id.star4)
        val star5      = findViewById<TextView>(R.id.star5)
        val etComment  = findViewById<EditText>(R.id.etRateComment)
        val btnSubmit  = findViewById<Button>(R.id.btnSubmitRating)
        val btnSkip    = findViewById<Button>(R.id.btnSkipRating)

        starButtons.addAll(listOf(star1, star2, star3, star4, star5))

        // Adjust colors based on dark/light mode
        val textPrimary = if (isDark) Color.WHITE else Color.parseColor("#1A1A1A")
        val textSecond  = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#555555")
        val bgColor     = if (isDark) Color.parseColor("#1C1C1C") else Color.WHITE
        val editBg      = if (isDark) Color.parseColor("#2C2C2C") else Color.parseColor("#F5F5F5")

        findViewById<android.view.View>(android.R.id.content)
            .rootView.setBackgroundColor(bgColor)

        tvTitle.setTextColor(textPrimary)
        tvSubtitle.setTextColor(textSecond)
        btnSkip.setTextColor(if (isDark) Color.parseColor("#888888") else Color.parseColor("#999999"))

        etComment.apply {
            setTextColor(textPrimary)
            setHintTextColor(if (isDark) Color.parseColor("#666666") else Color.parseColor("#AAAAAA"))
            setBackgroundColor(editBg)
        }

        // Customize title and subtitle depending on whether this is for an order or a booking
        tvTitle.text = if (type == "order") "Rate Your Order" else "Rate Your Experience"
        tvSubtitle.text = if (type == "order")
            "How satisfied are you with your product order?"
        else
            "How was your experience with $barber?\nService: $service"

        // When a star is tapped, record how many stars were selected and refresh the UI
        starButtons.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedStars = index + 1
                updateStars()
            }
        }

        btnSubmit.setOnClickListener {
            if (selectedStars == 0) {
                Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = etComment.text.toString().trim()
            val db      = Database(this)

            // Save the rating — label product orders differently from barber bookings
            db.saveRating(
                userEmail = email,
                barber    = if (type == "order") "Product Order" else barber,
                score     = selectedStars,
                comment   = comment,
                service   = if (type == "order") "Product Order" else service
            )
            Toast.makeText(this, "Thank you for your feedback! ⭐", Toast.LENGTH_SHORT).show()
            goToDashboard(email)
        }

        // Skip button — go to dashboard without saving anything
        btnSkip.setOnClickListener { goToDashboard(email) }
    }

    // Fills in the stars up to the selected count and greys out the rest
    private fun updateStars() {
        starButtons.forEachIndexed { index, star ->
            star.text = if (index < selectedStars) "★" else "☆"
            star.setTextColor(
                if (index < selectedStars) Color.parseColor("#FFD700")
                else Color.parseColor("#CCCCCC")
            )
        }
    }

    // Redirect back to dashboard and clear the back stack so they can't come back here
    private fun goToDashboard(email: String) {
        startActivity(Intent(this, Dashboard::class.java).apply {
            putExtra("email", email)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
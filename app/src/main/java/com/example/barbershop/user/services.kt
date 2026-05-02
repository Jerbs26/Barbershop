package com.example.barbershop.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.AppData
import com.example.barbershop.util.UserSidebarHelper

// Services screen
class Services : AppCompatActivity() {

    private lateinit var btnNewBooking    : Button
    private lateinit var btnBarbers       : Button
    private lateinit var btnServices      : Button
    private lateinit var btnProducts      : Button
    private lateinit var btnHistory       : Button
    private lateinit var btnRatings       : Button
    private lateinit var btnLogout        : Button
    private lateinit var servicesContainer: LinearLayout
    private val hamburger = UserSidebarHelper()

    // Theme colors
    private var cardBgColor     = Color.WHITE
    private var cardStrokeColor = Color.LTGRAY
    private var textPrimaryColor = Color.BLACK
    private var textSecondColor  = Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        // Grab the correct colors from the active theme so cards look right in dark mode too
        cardBgColor      = themeColor(com.google.android.material.R.attr.colorSurface)
        cardStrokeColor  = themeColor(com.google.android.material.R.attr.colorOutlineVariant)
        textPrimaryColor = themeColor(android.R.attr.textColorPrimary)
        textSecondColor  = themeColor(android.R.attr.textColorSecondary)

        hamburger.setup(
            context     = this,
            sidebar     = findViewById(R.id.sidebar),
            contentArea = findViewById(R.id.contentArea)
        )

        btnNewBooking     = findViewById(R.id.btnNewBooking)
        btnBarbers        = findViewById(R.id.btnBarbers)
        btnServices       = findViewById(R.id.btnServices)
        btnProducts       = findViewById(R.id.btnProducts)
        btnHistory        = findViewById(R.id.btnHistory)
        btnRatings        = findViewById(R.id.btnRatings)
        btnLogout         = findViewById(R.id.btnLogout)
        servicesContainer = findViewById(R.id.servicesContainer)

        loadUserSidebar()
        setupNavigation()
    }

    // Reload services from the database every time the screen becomes visible
    // This way any changes an admin made will show up without restarting the app
    override fun onResume() {
        super.onResume()

        AppData.reload(this) {
            if (!isFinishing && !isDestroyed) {
                runOnUiThread { renderServiceCards() }
            }
        }
    }

    // Populate the sidebar with the user's initials and email
    private fun loadUserSidebar() {
        val prefs    = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email    = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = email.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase()
        else                      namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        findViewById<TextView>(R.id.userEmail).text     = email
    }

    // Helper to resolve a theme attribute to its actual color value
    private fun themeColor(attr: Int, fallback: Int = Color.TRANSPARENT): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(attr, tv, true)) tv.data else fallback
    }

    // Clear the container and rebuild all service cards from the current AppData snapshot
    private fun renderServiceCards() {
        servicesContainer.removeAllViews()

        val services = synchronized(AppData.servicesList) { AppData.servicesList.toList() }

        if (services.isEmpty()) {
            servicesContainer.addView(TextView(this).apply {
                text = "No services available at the moment."
                textSize = 13f
                setTextColor(textSecondColor)
                setPadding(0, dpToPx(16), 0, 0)
            })
            return
        }

        services.forEach { svc ->
            servicesContainer.addView(buildServiceCard(svc))
        }
    }

    // Build one service card with name, description, duration, and price
    private fun buildServiceCard(svc: Map<String, String>): LinearLayout {
        val priceColor = Color.parseColor("#4A90E2")

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(10).toFloat()
                setStroke(dpToPx(1), cardStrokeColor)
            }
            elevation = dpToPx(2).toFloat()
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // Service name — bold and prominent at the top
        card.addView(TextView(this).apply {
            text     = svc["name"] ?: ""
            textSize = 16f
            setTextColor(textPrimaryColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(6)) }
        })

        // Only show description if it has content
        val desc = svc["desc"] ?: ""
        if (desc.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = desc
                textSize = 12f
                setTextColor(textSecondColor)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
            })
        }

        // Duration shown with a clock emoji for quick scanning
        card.addView(TextView(this).apply {
            text = "⏱ Duration: ${svc["duration"] ?: ""}"
            textSize = 11f
            setTextColor(textSecondColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
        })

        // Price — larger and in blue so it stands out
        card.addView(TextView(this).apply {
            text     = svc["price"] ?: ""
            textSize = 18f
            setTextColor(priceColor)
            setTypeface(null, Typeface.BOLD)
        })

        return card
    }

    // Guard against showing dialogs on a dead Activity
    private fun showLogoutDialog() {
        if (isFinishing || isDestroyed) return

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

    // Sidebar nav — close the drawer before launching the next screen
    private fun setupNavigation() {
        btnNewBooking.setOnClickListener { hamburger.closeSidebar(); startActivity(Intent(this, Dashboard::class.java)) }
        btnBarbers.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Barbers::class.java)) }
        btnServices.setOnClickListener   { hamburger.closeSidebar() }
        btnProducts.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Products::class.java)) }
        btnHistory.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, History::class.java)) }
        btnRatings.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Ratings::class.java)) }
        btnLogout.setOnClickListener     { showLogoutDialog() }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
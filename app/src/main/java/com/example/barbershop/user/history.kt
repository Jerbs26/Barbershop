package com.example.barbershop.user

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.util.Database
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.UserSidebarHelper

class History : AppCompatActivity() {

    private lateinit var btnNewBooking: Button
    private lateinit var btnBarbers: Button
    private lateinit var btnServices: Button
    private lateinit var btnProducts: Button
    private lateinit var btnHistory: Button
    private lateinit var btnRatings: Button
    private lateinit var btnLogout: Button
    private lateinit var historyContainer: LinearLayout
    private val hamburger = UserSidebarHelper()
    private var loggedInEmail: String = ""
    private val isDark get() = DarkModeHelper.isDarkMode(this)
    private val cardBg get() = if (isDark) Color.parseColor("#2C2C2C") else Color.WHITE
    private val textPrimary get() = if (isDark) Color.WHITE else Color.parseColor("#000000")
    private val textSecond get() = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")
    private val accentBlue get() = if (isDark) Color.parseColor("#7B9FF9") else Color.parseColor("#5B7FE8")

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

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
        historyContainer = findViewById(R.id.historyContainer)

        loadUserSidebar()
        loadHistory()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadUserSidebar() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        loggedInEmail = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = loggedInEmail.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase() else namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        // activity_history.xml uses tvUserEmail — single consistent id, no try/catch needed
        findViewById<TextView>(R.id.tvUserEmail).text = loggedInEmail
    }

    private fun loadHistory() {
        val db = Database(this)
        historyContainer.removeAllViews()

        val bookings = db.getBookingsByUser(loggedInEmail)
        val orders   = db.getOrdersByUser(loggedInEmail)
        db.close()

        if (bookings.isEmpty() && orders.isEmpty()) {
            historyContainer.addView(TextView(this).apply {
                text = "No bookings or orders yet."
                textSize = 14f
                setTextColor(textSecond)
                gravity = Gravity.CENTER
                setPadding(16, 64, 16, 64)
            })
            return
        }

        for (booking in bookings) historyContainer.addView(createBookingCard(booking))
        for (order   in orders)   historyContainer.addView(createOrderCard(order))
    }

    private fun createBookingCard(booking: Map<String, String>): CardView {
        val db          = Database(this)
        val status      = booking["status"] ?: "Pending"
        val isCompleted = status.equals("Completed", ignoreCase = true)
        val barber      = booking["barber"] ?: ""
        val service     = booking["service"] ?: ""
        val alreadyRated = isCompleted && db.hasRated(loggedInEmail, barber, service)
        db.close()

        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(cardBg)
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
        }
        headerRow.addView(TextView(this).apply {
            text = "SERVICE"; textSize = 9f; setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#5B7FE8"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        })
        headerRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        headerRow.addView(TextView(this).apply {
            text = status; textSize = 9f; setTextColor(Color.WHITE)
            setBackgroundColor(statusColor(status))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        })
        inner.addView(headerRow)

        addText(inner, "Booking #${booking["id"]}", 11f, textPrimary, true)
        addText(inner, "Barber: $barber",           10f, textSecond)
        addText(inner, "Service: $service",         10f, textSecond)
        addText(inner, "Date: ${booking["date"]} at ${booking["time"]}", 10f, textSecond)
        val amt = booking["amount"] ?: ""
        addText(inner, "Amount: ${if (amt.startsWith("₱")) amt else "₱$amt"}", 12f, accentBlue, true)

        if (isCompleted && !alreadyRated) {
            inner.addView(buildRateButton {
                startActivity(Intent(this, RateExperience::class.java).apply {
                    putExtra("email",   loggedInEmail)
                    putExtra("barber",  barber)
                    putExtra("service", service)
                    putExtra("type",    "booking")
                })
            })
        } else if (isCompleted) {
            inner.addView(TextView(this).apply {
                text = "✓ Rated"
                textSize = 11f
                setTextColor(Color.parseColor("#4CAF50"))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(8); it.gravity = Gravity.END }
            })
        }

        card.addView(inner)
        return card
    }

    private fun createOrderCard(order: Map<String, String>): CardView {
        val db          = Database(this)
        val status      = order["status"] ?: "Processing"
        val isCompleted = status.equals("Completed", ignoreCase = true)
        val items       = order["items"] ?: ""
        val alreadyRated = isCompleted && db.hasRated(loggedInEmail, "Product Order", items)
        db.close()

        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(cardBg)
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
        }
        headerRow.addView(TextView(this).apply {
            text = "🛍️ ORDER"; textSize = 9f; setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF9800"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        })
        headerRow.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        headerRow.addView(TextView(this).apply {
            text = status; textSize = 9f; setTextColor(Color.WHITE)
            setBackgroundColor(statusColor(status))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        })
        inner.addView(headerRow)

        addText(inner, "Order #${order["id"]}",  11f, textPrimary, true)
        addText(inner, "Items: $items",          10f, textSecond)
        addText(inner, "Date: ${order["date"]}", 10f, textSecond)
        val total = order["total"] ?: ""
        addText(inner, "Amount: ${if (total.startsWith("₱")) total else "₱$total"}", 12f, accentBlue, true)

        if (isCompleted && !alreadyRated) {
            inner.addView(buildRateButton {
                startActivity(Intent(this, RateExperience::class.java).apply {
                    putExtra("email",   loggedInEmail)
                    putExtra("barber",  "Product Order")
                    putExtra("service", items)
                    putExtra("type",    "order")
                })
            })
        } else if (isCompleted) {
            inner.addView(TextView(this).apply {
                text = "✓ Rated"
                textSize = 11f
                setTextColor(Color.parseColor("#4CAF50"))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(8); it.gravity = Gravity.END }
            })
        }

        card.addView(inner)
        return card
    }

    private fun statusColor(status: String) = when (status.lowercase()) {
        "completed" -> Color.parseColor("#4CAF50")
        "cancelled" -> Color.parseColor("#F44336")
        "confirmed" -> Color.parseColor("#2196F3")
        else        -> Color.parseColor("#FF9800")
    }

    private fun buildRateButton(onClick: () -> Unit): Button {
        return Button(this).apply {
            text = "Rate this"
            textSize = 11f
            setTextColor(Color.parseColor("#000000"))
            setTypeface(null, Typeface.BOLD)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFD700"))
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(10); it.gravity = Gravity.END }
            setOnClickListener { onClick() }
        }
    }

    private fun addText(parent: LinearLayout, text: String, size: Float, color: Int, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(4)) }
        })
    }

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

    private fun setupNavigation() {
        btnNewBooking.setOnClickListener { hamburger.closeSidebar(); startActivity(Intent(this, Dashboard::class.java)) }
        btnBarbers.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Barbers::class.java)) }
        btnServices.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Services::class.java)) }
        btnProducts.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Products::class.java)) }
        btnHistory.setOnClickListener    { hamburger.closeSidebar() }
        btnRatings.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Ratings::class.java)) }
        btnLogout.setOnClickListener     { showLogoutDialog() }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
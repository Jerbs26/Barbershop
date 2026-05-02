package com.example.barbershop.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.util.Database
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.UserSidebarHelper

class Barbers : AppCompatActivity() {

    private lateinit var btnNewBooking: Button
    private lateinit var btnBarbers: Button
    private lateinit var btnServices: Button
    private lateinit var btnProducts: Button
    private lateinit var btnHistory: Button
    private lateinit var btnRatings: Button
    private lateinit var btnLogout: Button
    private lateinit var barbersContainer: LinearLayout
    private val hamburger = UserSidebarHelper()
    private val pollHandler = Handler(Looper.getMainLooper())
    private var lastBarberFingerprint = ""

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForDataChanges()
            pollHandler.postDelayed(this, 3_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barbers)

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
        barbersContainer = findViewById(R.id.barbersContainer)

        loadUserSidebar()
        setupNavigation()
        loadBarbers()
    }

    override fun onResume() {
        super.onResume()
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    // Fingerprint helper
    private fun barberFingerprint(list: List<Map<String, String>>) =
        list.joinToString("|") { "${it["id"]},${it["name"]},${it["status"]}" }

    // Real-time polling
    private fun checkForDataChanges() {
        Thread {
            val db = Database(this)
            val freshBarbers = try {
                db.getAllBarbers().filter { it["status"] == "Active" }
            } catch (e: Exception) {
                null
            } finally {
                db.close()
            }

            if (freshBarbers != null) {
                val newPrint = barberFingerprint(freshBarbers)
                if (newPrint != lastBarberFingerprint) {
                    runOnUiThread {
                        lastBarberFingerprint = newPrint
                        renderBarbers(freshBarbers)
                    }
                }
            }
        }.start()
    }

    // Load & render barbers
    private fun loadBarbers() {
        val db = Database(this)
        val activeBarbers = try {
            db.getAllBarbers().filter { it["status"] == "Active" }
        } catch (e: Exception) {
            emptyList()
        } finally {
            db.close()
        }
        lastBarberFingerprint = barberFingerprint(activeBarbers)
        renderBarbers(activeBarbers)
    }

    private fun renderBarbers(activeBarbers: List<Map<String, String>>) {
        barbersContainer.removeAllViews()

        val colorSurface   = themeColor(com.google.android.material.R.attr.colorSurface)
        val colorOnSurface = themeColor(com.google.android.material.R.attr.colorOnSurface)
        val isDark         = DarkModeHelper.isDarkMode(this)
        val colorSubtext   = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#777777")

        if (activeBarbers.isEmpty()) {
            barbersContainer.addView(TextView(this).apply {
                text = "No barbers available at the moment."
                textSize = 14f
                setTextColor(colorSubtext)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(48), 0, 0)
            })
            return
        }

        val avatarColors = listOf(
            "#4A90E2", "#E74C3C", "#F5A623",
            "#4CAF50", "#9B59B6", "#1ABC9C", "#E67E22"
        )

        activeBarbers.forEachIndexed { index, barber ->
            val name      = barber["name"]      ?: ""
            val specialty = barber["specialty"] ?: ""
            val desc      = barber["desc"]      ?: ""
            val exp       = barber["exp"]       ?: ""

            val card = CardView(this).apply {
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(3).toFloat()
                setCardBackgroundColor(colorSurface)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                gravity = Gravity.CENTER_VERTICAL
            }

            val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
            row.addView(TextView(this).apply {
                text = initials
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(avatarColors[index % avatarColors.size]))
                }
                layoutParams = LinearLayout.LayoutParams(dpToPx(64), dpToPx(64))
                    .also { it.setMargins(0, 0, dpToPx(14), 0) }
            })

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            info.addView(TextView(this).apply {
                text = name
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(colorOnSurface)
            })

            if (specialty.isNotBlank()) info.addView(TextView(this).apply {
                text = "Specialty: $specialty"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#4A90E2"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(4), 0, 0) }
            })

            if (desc.isNotBlank()) info.addView(TextView(this).apply {
                text = desc
                textSize = 11f
                setTextColor(colorSubtext)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(4), 0, 0) }
            })

            if (exp.isNotBlank()) info.addView(TextView(this).apply {
                text = "Experience: $exp"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(colorOnSurface)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(6), 0, 0) }
            })

            row.addView(info)
            card.addView(row)
            barbersContainer.addView(card)
        }
    }

    // Helpers
    private fun themeColor(attr: Int): Int {
        val ta = theme.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun loadUserSidebar() {
        val prefs    = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email    = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = email.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase() else namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        findViewById<TextView>(R.id.userEmail).text     = email
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
        btnBarbers.setOnClickListener    { hamburger.closeSidebar() }
        btnServices.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Services::class.java)) }
        btnProducts.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Products::class.java)) }
        btnHistory.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, History::class.java)) }
        btnRatings.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Ratings::class.java)) }
        btnLogout.setOnClickListener     { showLogoutDialog() }
    }
}
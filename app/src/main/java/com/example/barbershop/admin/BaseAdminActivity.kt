package com.example.barbershop.admin

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.R
import com.example.barbershop.MainActivity
import com.example.barbershop.auth.logIn
import com.example.barbershop.util.AppData
import com.example.barbershop.util.Database
import com.example.barbershop.util.OtpEmailSender

abstract class BaseAdminActivity : AppCompatActivity() {

    protected lateinit var sidebar: LinearLayout
    protected lateinit var contentArea: LinearLayout
    private lateinit var btnAdminDashboard: LinearLayout
    private lateinit var btnAdminAnalytics: LinearLayout
    private lateinit var btnAdminBookings: LinearLayout
    private lateinit var btnAdminBarbers: LinearLayout
    private lateinit var btnAdminServices: LinearLayout
    private lateinit var btnAdminProducts: LinearLayout
    private lateinit var btnAdminOrders: LinearLayout
    private lateinit var btnAdminFeedback: LinearLayout
    private lateinit var btnAdminSchedules: LinearLayout
    private lateinit var btnAdminLogout: LinearLayout

    private var isSidebarOpen = true
    private var baseSetupDone = false

    protected val screenWidthDp: Int get() {
        val dm = resources.displayMetrics
        return (dm.widthPixels / dm.density).toInt()
    }
    protected val isSmallScreen: Boolean get() = screenWidthDp < 480

    protected val db: Database by lazy { Database(applicationContext) }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        if (baseSetupDone) {
            AppData.reload(applicationContext)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { db.close() } catch (_: Exception) {}
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    protected fun setupBase(activeNavId: Int) {
        val prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean("IS_ADMIN", false)) {
            startActivity(Intent(this, logIn::class.java))
            finish()
            return
        }

        AppData.load(applicationContext)

        sidebar           = findViewById(R.id.sidebar)
        contentArea       = findViewById(R.id.contentArea)
        btnAdminDashboard = findViewById(R.id.btnAdminDashboard)
        btnAdminAnalytics = findViewById(R.id.btnAdminAnalytics)
        btnAdminBookings  = findViewById(R.id.btnAdminBookings)
        btnAdminBarbers   = findViewById(R.id.btnAdminBarbers)
        btnAdminServices  = findViewById(R.id.btnAdminServices)
        btnAdminProducts  = findViewById(R.id.btnAdminProducts)
        btnAdminOrders    = findViewById(R.id.btnAdminOrders)
        btnAdminFeedback  = findViewById(R.id.btnAdminFeedback)
        btnAdminSchedules = findViewById(R.id.btnAdminSchedules)
        btnAdminLogout    = findViewById(R.id.btnAdminLogout)

        if (isSmallScreen) {
            sidebar.visibility = View.GONE
            isSidebarOpen = false
            addHamburgerButton()
        }

        highlightNav(activeNavId)
        setupNavClicks()

        baseSetupDone = true
    }

    private fun addHamburgerButton() {
        val btn = Button(this).apply {
            text = "☰"
            textSize = 22f
            setTextColor(Color.parseColor("#F5A623"))
            background = null
            backgroundTintList = null
            stateListAnimator = null
            setPadding(0, 0, 0, 0)
            minWidth = 0
            minHeight = 0
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
                .also { it.setMargins(0, 0, 0, dpToPx(12)) }
            setOnClickListener {
                if (isSidebarOpen) {
                    sidebar.visibility = View.GONE
                    isSidebarOpen = false
                } else {
                    sidebar.visibility = View.VISIBLE
                    isSidebarOpen = true
                }
            }
        }
        contentArea.addView(btn, 0)
    }

    private fun highlightNav(activeId: Int) {
        listOf(
            btnAdminDashboard, btnAdminAnalytics, btnAdminBookings,
            btnAdminBarbers,   btnAdminServices,  btnAdminProducts,
            btnAdminOrders,    btnAdminFeedback,  btnAdminSchedules
        ).forEach { it.setBackgroundColor(Color.TRANSPARENT) }
        findViewById<LinearLayout>(activeId)?.setBackgroundColor(Color.parseColor("#3A3A55"))
    }

    private fun setupNavClicks() {
        fun go(cls: Class<*>) {
            if (this::class.java != cls) {
                startActivity(Intent(this, cls).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                })
            }
            if (isSmallScreen) {
                sidebar.visibility = View.GONE
                isSidebarOpen = false
            }
        }
        btnAdminDashboard.setOnClickListener { go(AdminDashboard::class.java) }
        btnAdminAnalytics.setOnClickListener { go(AdminAnalytics::class.java) }
        btnAdminBookings.setOnClickListener  { go(AdminBookings::class.java) }
        btnAdminBarbers.setOnClickListener   { go(AdminBarbers::class.java) }
        btnAdminServices.setOnClickListener  { go(AdminServices::class.java) }
        btnAdminProducts.setOnClickListener  { go(AdminProducts::class.java) }
        btnAdminOrders.setOnClickListener    { go(AdminOrders::class.java) }
        btnAdminFeedback.setOnClickListener  { go(AdminFeedback::class.java) }
        btnAdminSchedules.setOnClickListener { go(AdminSchedules::class.java) }
        btnAdminLogout.setOnClickListener    { showLogoutDialog() }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                getSharedPreferences("AdminPrefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Status update helpers
    protected fun persistBookingStatus(booking: MutableMap<String, String>, newStatus: String) {
        val id = booking["id"] ?: return

        Thread {
            try { db.updateBookingStatus(id.toLongOrNull() ?: return@Thread, newStatus) }
            catch (e: Exception) { e.printStackTrace() }
        }.start()

        booking["status"] = newStatus

        if (newStatus == "Confirmed") {
            val email    = booking["email"]    ?: ""
            val username = booking["username"]?.takeIf { it.isNotBlank() } ?: "Customer"
            val barber   = booking["barber"]   ?: ""
            val service  = booking["service"]  ?: ""
            val date     = booking["date"]     ?: ""
            val time     = booking["time"]     ?: ""
            val amount   = booking["amount"]   ?: ""

            if (email.isNotBlank()) {
                Thread {
                    try {
                        OtpEmailSender.sendBookingConfirmation(
                            recipientEmail = email,
                            username       = username,
                            barber         = barber,
                            service        = service,
                            date           = date,
                            timeSlot       = time,
                            amount         = amount
                        )
                        Log.d("Email", "Booking confirmation sent to $email")
                    } catch (e: Exception) {
                        Log.e("Email", "Failed to send booking confirmation: ${e.message}")
                    }
                }.start()
            }
        }
    }

    protected fun persistOrderStatus(order: MutableMap<String, String>, newStatus: String) {
        val id = order["id"] ?: return

        Thread {
            try { db.updateOrderStatus(id, newStatus) }
            catch (e: Exception) { e.printStackTrace() }
        }.start()

        order["status"] = newStatus

        if (newStatus == "Completed") {
            val email    = order["email"]    ?: ""
            val customer = order["customer"]?.takeIf { it.isNotBlank() } ?: "Customer"
            val orderId  = order["id"]       ?: ""
            val items    = order["items"]    ?: ""
            val total    = order["total"]    ?: ""

            if (email.isNotBlank()) {
                Thread {
                    try {
                        OtpEmailSender.sendOrderReadyNotification(
                            recipientEmail = email,
                            customerName   = customer,
                            orderId        = orderId,
                            items          = items,
                            total          = total
                        )
                        Log.d("Email", "Order ready notification sent to $email")
                    } catch (e: Exception) {
                        Log.e("Email", "Failed to send order ready email: ${e.message}")
                    }
                }.start()
            }
        }
    }

    // Shared UI helpers

    protected fun makePageTitle(title: String): TextView =
        makeText(title, 20f, "#1A1A1A", true).also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { lp -> lp.setMargins(0, 0, 0, dpToPx(16)) }
        }

    protected fun makeHorizontalRow(bottomMargin: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, bottomMargin) }
        }

    protected fun verticalLayout(bottomMargin: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, bottomMargin) }
        }

    protected fun makeCard(): CardView =
        CardView(this).apply {
            radius = dpToPx(10).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
        }

    protected fun makeText(text: String, size: Float, color: String, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text; textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    protected fun makeStatCard(label: String, value: String, color: String, weight: Float): CardView =
        CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                .also { it.setMargins(0, 0, dpToPx(8), 0) }
            radius = dpToPx(10).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            val colorBar = LinearLayout(this@BaseAdminActivity).apply {
                setBackgroundColor(Color.parseColor(color))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(4))
            }
            val inner = LinearLayout(this@BaseAdminActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
            }
            inner.addView(makeText(label, 9f, "#888888"))
            inner.addView(makeText(value, 16f, "#1A1A1A", true))
            val wrapper = LinearLayout(this@BaseAdminActivity).apply { orientation = LinearLayout.VERTICAL }
            wrapper.addView(colorBar)
            wrapper.addView(inner)
            addView(wrapper)
        }

    protected fun makeActionButton(label: String, color: String): Button =
        Button(this).apply {
            text = label; textSize = 10f
            setTextColor(Color.WHITE)
            background = roundedBg(color)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(30))
        }

    protected fun makeStatusBadge(status: String): TextView {
        val colorMap = mapOf(
            "Confirmed"    to "#4A90E2",
            "Completed"    to "#4CAF50",
            "Cancelled"    to "#E74C3C",
            "Pending"      to "#F5A623",
            "Active"       to "#4CAF50",
            "Inactive"     to "#E74C3C",
            "In Stock"     to "#4CAF50",
            "Low Stock"    to "#F5A623",
            "Out of Stock" to "#E74C3C",
            "Processing"   to "#9B59B6",
            "Rejected"     to "#E74C3C"
        )
        return TextView(this).apply {
            text = status; textSize = 9f
            setTextColor(Color.WHITE)
            setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3))
            gravity    = Gravity.CENTER
            background = roundedBg(colorMap[status] ?: "#F5A623")
        }
    }

    protected fun makeDivider(): View =
        View(this).apply {
            setBackgroundColor(Color.parseColor("#F0F0F0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        }

    protected fun makeDialogEditText(labelText: String, initialValue: String): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
        }
        wrapper.addView(makeText(labelText, 12f, "#555555").also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(4))
        })
        val et = EditText(this).apply {
            setText(initialValue); textSize = 13f
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            background = roundedBg("#FFFFFF", strokeColor = "#CCCCCC")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        wrapper.addView(et)
        wrapper.tag = et
        return wrapper
    }

    protected fun showDeleteConfirm(type: String, name: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete $type?")
            .setMessage("Are you sure you want to delete \"$name\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    protected fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    protected fun roundedBg(
        fillColor: String,
        strokeColor: String? = null,
        radius: Float = dpToPx(6).toFloat()
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(fillColor))
            cornerRadius = radius
            strokeColor?.let { setStroke(dpToPx(1), Color.parseColor(it)) }
        }

    protected fun circleBg(fillColor: String): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(fillColor))
        }

    protected fun avatarColor(index: Int): String {
        val colors = listOf(
            "#4A90E2", "#E74C3C", "#F5A623",
            "#4CAF50", "#9B59B6", "#1ABC9C", "#E67E22"
        )
        return colors[index % colors.size]
    }

    protected fun parsePeso(value: String?): Double =
        value?.replace("₱", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0

    protected fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
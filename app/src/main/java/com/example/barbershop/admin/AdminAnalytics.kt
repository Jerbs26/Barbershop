package com.example.barbershop.admin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.barbershop.util.AppData
import com.example.barbershop.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminAnalytics : BaseAdminActivity() {

    private val refreshHandler  = Handler(Looper.getMainLooper())
    @Volatile private var isRefreshing = false
    private val refreshInterval = 10_000L

    private val refreshRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                silentRefresh()
                refreshHandler.postDelayed(this, refreshInterval)
            }
        }
    }

    private val allBookings = mutableListOf<MutableMap<String, String>>()
    private val allOrders   = mutableListOf<MutableMap<String, String>>()
    private val allFeedback = mutableListOf<MutableMap<String, String>>()
    private var filterStart: Date? = null
    private var filterEnd:   Date? = null
    private var filterLabel  = "All Time"
    private var activeTab    = 0

    private val dispFmt  = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_analytics)
        setupBase(R.id.btnAdminAnalytics)
    }

    override fun onResume() {
        super.onResume()
        clearContent()
        loadContent()
        refreshHandler.postDelayed(refreshRunnable, refreshInterval)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onDestroy()
    }

    private fun clearContent() {
        val startIndex = if (isSmallScreen) 1 else 0
        while (contentArea.childCount > startIndex) contentArea.removeViewAt(startIndex)
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Analytics"))
        val loadingText = makeText("Loading...", 13f, "#888888")
        contentArea.addView(loadingText)

        Thread {
            try {
                fetchFromDb()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        contentArea.removeView(loadingText)
                        renderAll()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        contentArea.removeView(loadingText)
                        contentArea.addView(makeText("Failed to load analytics.", 13f, "#E53935"))
                    }
                }
            }
        }.start()
    }

    private fun silentRefresh() {
        if (isRefreshing) return
        isRefreshing = true
        Thread {
            try {
                fetchFromDb()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        clearContent()
                        contentArea.addView(makePageTitle("Analytics"))
                        renderAll()
                    }
                    isRefreshing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { isRefreshing = false }
            }
        }.start()
    }

    private fun fetchFromDb() {
        val freshBookings = mutableListOf<MutableMap<String, String>>()
        val freshOrders   = mutableListOf<MutableMap<String, String>>()
        val freshFeedback = mutableListOf<MutableMap<String, String>>()

        db.getAllBookings().forEach { row ->
            freshBookings.add(mutableMapOf(
                "id"      to (row["id"]      ?: ""),
                "barber"  to (row["barber"]  ?: ""),
                "service" to (row["service"] ?: ""),
                "amount"  to (row["amount"]  ?: ""),
                "status"  to (row["status"]  ?: "Pending"),
                "date"    to (row["date"]    ?: "")
            ))
        }
        db.getAllOrders().forEach { row ->
            freshOrders.add(mutableMapOf(
                "id"     to (row["id"]      ?: ""),
                "total"  to (row["total"]   ?: ""),
                "status" to (row["status"]  ?: "Confirmed"),
                "date"   to (row["date"]    ?: ""),
                "items"  to (row["items"]   ?: row["product"] ?: row["name"] ?: "")
            ))
        }
        db.getAllRatings().forEach { row ->
            freshFeedback.add(mutableMapOf("rating" to (row["score"] ?: "0")))
        }

        synchronized(allBookings) { allBookings.clear(); allBookings.addAll(freshBookings) }
        synchronized(allOrders)   { allOrders.clear();   allOrders.addAll(freshOrders)     }
        synchronized(allFeedback) { allFeedback.clear(); allFeedback.addAll(freshFeedback) }

        synchronized(AppData.bookingsList) { AppData.bookingsList.clear(); AppData.bookingsList.addAll(freshBookings) }
        synchronized(AppData.ordersList)   { AppData.ordersList.clear();   AppData.ordersList.addAll(freshOrders)     }
        synchronized(AppData.feedbackList) { AppData.feedbackList.clear(); AppData.feedbackList.addAll(freshFeedback) }
    }

    private fun applyFilter(label: String, start: Date, end: Date) {
        filterLabel = label; filterStart = start; filterEnd = end
        clearContent()
        contentArea.addView(makePageTitle("Analytics"))
        renderAll()
    }

    private fun clearFilter() {
        filterLabel = "All Time"; filterStart = null; filterEnd = null
        clearContent()
        contentArea.addView(makePageTitle("Analytics"))
        renderAll()
    }

    private fun filteredBookings(): List<MutableMap<String, String>> {
        val s = filterStart ?: return allBookings.toList()
        val e = filterEnd   ?: return allBookings.toList()
        if (s.after(e)) return emptyList()
        return allBookings.filter { b ->
            val d = parseDate(b["date"] ?: "") ?: return@filter false
            !d.before(s) && !d.after(e)
        }
    }

    private fun filteredOrders(): List<MutableMap<String, String>> {
        val s = filterStart ?: return allOrders.toList()
        val e = filterEnd   ?: return allOrders.toList()
        if (s.after(e)) return emptyList()
        return allOrders.filter { o ->
            val d = parseDate(o["date"] ?: "") ?: return@filter false
            !d.before(s) && !d.after(e)
        }
    }

    private fun renderAll() {
        contentArea.addView(buildFilterCard())
        contentArea.addView(buildShowingBanner())
        renderAnalytics(filteredBookings(), filteredOrders(), allFeedback.toList())
    }

    private fun buildFilterCard(): android.widget.FrameLayout {
        val wrapper = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(10) }
            background    = roundedBg("#FFFFFF", strokeColor = "#E0E0E0")
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val tabRow = LinearLayout(this).apply {
            orientation  = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(14) }
        }

        val tabLabels = listOf("Quick Select", "Single Date", "Date Range")

        fun buildTab(index: Int): TextView = TextView(this).apply {
            text     = tabLabels[index]
            textSize = 11f
            gravity  = Gravity.CENTER
            isAllCaps = false
            val isActive = index == activeTab
            setTextColor(if (isActive) Color.parseColor("#4A90E2") else Color.parseColor("#888888"))
            setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f)
            if (isActive) {
                background = roundedBg("#EEF4FF", strokeColor = "#4A90E2")
            }
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            setOnClickListener {
                activeTab = index
                clearContent()
                contentArea.addView(makePageTitle("Analytics"))
                renderAll()
            }
        }

        tabRow.addView(buildTab(0))
        tabRow.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1)
        })
        tabRow.addView(buildTab(1))
        tabRow.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(6), 1)
        })
        tabRow.addView(buildTab(2))
        outer.addView(tabRow)

        when (activeTab) {
            0 -> outer.addView(buildQuickSelectContent())
            1 -> outer.addView(buildSingleDateContent())
            2 -> outer.addView(buildDateRangeContent())
        }

        wrapper.addView(outer)
        return wrapper
    }

    private fun buildQuickSelectContent(): LinearLayout {
        val vertical = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        data class QuickItem(val label: String, val build: () -> Pair<Date, Date>)

        val items = listOf(
            QuickItem("Today") {
                val s = cal().also { it.startOfDay() }.time
                val e = cal().also { it.endOfDay() }.time
                s to e
            },
            QuickItem("Yesterday") {
                val s = cal().also { it.add(Calendar.DAY_OF_YEAR, -1); it.startOfDay() }.time
                val e = cal().also { it.add(Calendar.DAY_OF_YEAR, -1); it.endOfDay() }.time
                s to e
            },
            QuickItem("Last 7 Days") {
                val s = cal().also { it.add(Calendar.DAY_OF_YEAR, -6); it.startOfDay() }.time
                val e = cal().also { it.endOfDay() }.time
                s to e
            },
            QuickItem("Last 30 Days") {
                val s = cal().also { it.add(Calendar.DAY_OF_YEAR, -29); it.startOfDay() }.time
                val e = cal().also { it.endOfDay() }.time
                s to e
            },
            QuickItem("This Month") {
                val s = cal().also { it.set(Calendar.DAY_OF_MONTH, 1); it.startOfDay() }.time
                val eCal = cal(); val e = eCal.also {
                it.set(Calendar.DAY_OF_MONTH, eCal.getActualMaximum(Calendar.DAY_OF_MONTH)); it.endOfDay()
            }.time
                s to e
            },
            QuickItem("Last Month") {
                val s = cal().also {
                    it.add(Calendar.MONTH, -1)
                    it.set(Calendar.DAY_OF_MONTH, 1); it.startOfDay()
                }.time
                val eCal = cal().also { it.add(Calendar.MONTH, -1) }
                val e = eCal.also {
                    it.set(Calendar.DAY_OF_MONTH, eCal.getActualMaximum(Calendar.DAY_OF_MONTH)); it.endOfDay()
                }.time
                s to e
            },
            QuickItem("This Year") {
                val s = cal().also { it.set(Calendar.DAY_OF_YEAR, 1); it.startOfDay() }.time
                val eCal = cal()
                val e = eCal.also {
                    it.set(Calendar.DAY_OF_YEAR, eCal.getActualMaximum(Calendar.DAY_OF_YEAR)); it.endOfDay()
                }.time
                s to e
            }
        )

        items.chunked(4).forEach { chunk ->
            val row = LinearLayout(this).apply {
                orientation  = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(6) }
            }
            chunk.forEach { item ->
                val isActive = filterLabel == item.label
                row.addView(Button(this).apply {
                    text      = item.label
                    textSize  = 10f
                    isAllCaps = false
                    setTextColor(if (isActive) Color.WHITE else Color.parseColor("#333333"))
                    background = roundedBg(if (isActive) "#4A90E2" else "#F0F0F0", radius = dpToPx(20).toFloat())
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(32), 1f)
                        .also { it.marginEnd = dpToPx(4) }
                    setPadding(dpToPx(4), 0, dpToPx(4), 0)
                    setOnClickListener {
                        val (s, e) = item.build()
                        applyFilter(item.label, s, e)
                    }
                })
            }
            repeat(4 - chunk.size) {
                row.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dpToPx(32), 1f)
                        .also { it.marginEnd = dpToPx(4) }
                })
            }
            vertical.addView(row)
        }

        return vertical
    }

    private fun buildSingleDateContent(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvDate = TextView(this).apply {
            text     = if (filterStart != null) dispFmt.format(filterStart!!) else "Tap to pick a date"
            textSize = 13f
            setTextColor(if (filterStart != null) Color.parseColor("#1A1A1A") else Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnPick = Button(this).apply {
            text      = "Pick Date"
            textSize  = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg("#4A90E2", radius = dpToPx(8).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(34)
            )
            setOnClickListener { showSingleDatePicker() }
        }

        layout.addView(tvDate)
        layout.addView(btnPick)
        return layout
    }

    private fun showSingleDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, y, m, d ->
                if (isFinishing || isDestroyed) return@DatePickerDialog
                val s = Calendar.getInstance().also { it.set(y, m, d, 0, 0, 0); it.set(Calendar.MILLISECOND, 0) }.time
                val e = Calendar.getInstance().also { it.set(y, m, d, 23, 59, 59); it.set(Calendar.MILLISECOND, 999) }.time
                activeTab = 1
                applyFilter(dispFmt.format(s), s, e)
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun buildDateRangeContent(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val startLabel = if (filterStart != null) dispFmt.format(filterStart!!) else "Start date"
        val endLabel   = if (filterEnd   != null) dispFmt.format(filterEnd!!)   else "End date"

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(8) }
        }

        fun dateBtn(label: String, onClick: () -> Unit): Button = Button(this).apply {
            text      = label
            textSize  = 11f
            isAllCaps = false
            setTextColor(if (label == "Start date" || label == "End date") Color.parseColor("#999999") else Color.parseColor("#1A1A1A"))
            background = roundedBg("#F5F5F5", strokeColor = "#CCCCCC", radius = dpToPx(8).toFloat())
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f)
                .also { it.marginEnd = dpToPx(8) }
            setOnClickListener { onClick() }
        }

        val btnStart = dateBtn(startLabel) { showRangeStartPicker() }
        val tvArrow  = makeText("→", 14f, "#888888").also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { lp -> lp.marginEnd = dpToPx(8) }
        }
        val btnEnd = dateBtn(endLabel) { showRangeEndPicker() }

        row.addView(btnStart)
        row.addView(tvArrow)
        row.addView(btnEnd)
        layout.addView(row)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        actionRow.addView(Button(this).apply {
            text      = "Apply Range"
            textSize  = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg("#4A90E2", radius = dpToPx(8).toFloat())
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f)
                .also { it.marginEnd = dpToPx(8) }
            setOnClickListener {
                val s = filterStart
                val e = filterEnd
                if (s != null && e != null) {
                    val label = "${dispFmt.format(s)} – ${dispFmt.format(e)}"
                    applyFilter(label, s, e)
                }
            }
        })
        actionRow.addView(Button(this).apply {
            text      = "Clear"
            textSize  = 11f
            isAllCaps = false
            setTextColor(Color.parseColor("#E53935"))
            background = roundedBg("#FFEBEE", strokeColor = "#E53935", radius = dpToPx(8).toFloat())
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f)
            setOnClickListener { clearFilter() }
        })
        layout.addView(actionRow)
        return layout
    }

    private fun showRangeStartPicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, y, m, d ->
                if (isFinishing || isDestroyed) return@DatePickerDialog
                filterStart = Calendar.getInstance().also {
                    it.set(y, m, d, 0, 0, 0); it.set(Calendar.MILLISECOND, 0)
                }.time
                activeTab = 2
                clearContent()
                contentArea.addView(makePageTitle("Analytics"))
                renderAll()
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showRangeEndPicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, y, m, d ->
                if (isFinishing || isDestroyed) return@DatePickerDialog
                filterEnd = Calendar.getInstance().also {
                    it.set(y, m, d, 23, 59, 59); it.set(Calendar.MILLISECOND, 999)
                }.time
                activeTab = 2
                clearContent()
                contentArea.addView(makePageTitle("Analytics"))
                renderAll()
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun buildShowingBanner(): LinearLayout {
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
            background      = roundedBg("#5C6BC0", radius = dpToPx(10).toFloat())
            clipToOutline   = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        val showingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(4) }
        }
        showingRow.addView(makeText("Showing: ", 13f, "#DDDDFF").also {
            (it as TextView).setTypeface(null, Typeface.NORMAL)
        })
        showingRow.addView(makeText(filterLabel, 15f, "#FFFFFF", true))
        banner.addView(showingRow)

        val dateRangeText = when {
            filterStart != null && filterEnd != null ->
                "${dispFmt.format(filterStart!!)} - ${dispFmt.format(filterEnd!!)}"
            else -> "All records"
        }
        banner.addView(makeText(dateRangeText, 11f, "#BBBBEE"))

        return banner
    }

    private fun renderAnalytics(
        bookings: List<MutableMap<String, String>>,
        orders:   List<MutableMap<String, String>>,
        feedback: List<MutableMap<String, String>>
    ) {
        val bookingRevenue = bookings.filter { it["status"] == "Completed" }
            .sumOf { parsePeso(it["amount"] ?: "") }
        val orderRevenue   = orders.filter { it["status"] == "Completed" }
            .sumOf { parsePeso(it["total"] ?: "") }
        val totalRevenue   = bookingRevenue + orderRevenue
        val totalOrders    = orders.filter { it["status"] == "Completed" }.size
        val totalBookings  = bookings.size
        val avgRating      = feedback.mapNotNull { it["rating"]?.toDoubleOrNull() }
            .let { if (it.isEmpty()) 0.0 else it.average() }

        if (isSmallScreen) {
            val row1 = makeHorizontalRow(dpToPx(10))
            row1.addView(makeStatCard("Total Revenue",  "₱${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", "#4CAF50", 1f))
            row1.addView(makeStatCard("Total Bookings", totalBookings.toString(),                                        "#4A90E2", 1f))
            contentArea.addView(row1)
            val row2 = makeHorizontalRow(dpToPx(16))
            row2.addView(makeStatCard("Orders Done", totalOrders.toString(),                                             "#F5A623", 1f))
            row2.addView(makeStatCard("Avg Rating",  String.format(Locale.getDefault(), "%.1f ★", avgRating),           "#9B59B6", 1f))
            contentArea.addView(row2)
        } else {
            val row = makeHorizontalRow(dpToPx(16))
            row.addView(makeStatCard("Total Revenue",  "₱${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", "#4CAF50", 1f))
            row.addView(makeStatCard("Total Bookings", totalBookings.toString(),                                        "#4A90E2", 1f))
            row.addView(makeStatCard("Orders Done",    totalOrders.toString(),                                          "#F5A623", 1f))
            row.addView(makeStatCard("Avg Rating",     String.format(Locale.getDefault(), "%.1f ★", avgRating),        "#9B59B6", 1f))
            contentArea.addView(row)
        }

        val revenueCard  = makeCard()
        val revenueInner = verticalLayout(0).also {
            it.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }
        revenueInner.addView(makeText("Revenue Breakdown", 15f, "#1A1A1A", true)
            .also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(10)) })
        listOf(
            "Booking Revenue"       to bookingRevenue,
            "Product Order Revenue" to orderRevenue,
            "Total Revenue"         to totalRevenue
        ).forEachIndexed { i, (label, amount) ->
            val row    = makeHorizontalRow(dpToPx(6))
            val isBold = i == 2
            row.addView(makeText(label, 13f, if (isBold) "#1A1A1A" else "#555555", isBold).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(makeText("₱${String.format(Locale.getDefault(), "%.2f", amount)}", 13f, "#4CAF50", isBold))
            revenueInner.addView(row)
            if (i < 2) revenueInner.addView(makeDivider())
        }
        revenueCard.addView(revenueInner)
        contentArea.addView(revenueCard)

        val statusCard  = makeCard()
        val statusInner = verticalLayout(0).also {
            it.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }
        statusInner.addView(makeText("Bookings by Status", 15f, "#1A1A1A", true)
            .also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(12)) })
        val statusGroups = bookings.groupBy { it["status"] ?: "Unknown" }
        val colorMap = mapOf(
            "Pending" to "#F5A623", "Confirmed" to "#4A90E2",
            "Completed" to "#4CAF50", "Cancelled" to "#E74C3C"
        )
        listOf("Pending", "Confirmed", "Completed", "Cancelled").forEach { status ->
            val count = statusGroups[status]?.size ?: 0
            val pct   = if (totalBookings > 0) (count * 100.0 / totalBookings).toInt().coerceIn(0, 100) else 0
            val row   = makeHorizontalRow(dpToPx(8))
            row.addView(makeStatusBadge(status).also {
                it.layoutParams = LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val barBg = LinearLayout(this).apply {
                orientation  = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(18), 1f)
                    .also { it.setMargins(dpToPx(8), 0, dpToPx(8), 0) }
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            val clampedPct = pct.coerceIn(0, 100)
            if (clampedPct > 0) {
                // Filled portion
                barBg.addView(LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, clampedPct.toFloat())
                    setBackgroundColor(Color.parseColor(colorMap[status] ?: "#888888"))
                })
            }

            barBg.addView(LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (100 - clampedPct).toFloat().coerceAtLeast(1f))
            })
            row.addView(barBg)
            row.addView(makeText("$count ($pct%)", 11f, "#555555"))
            statusInner.addView(row)
        }
        statusCard.addView(statusInner)
        contentArea.addView(statusCard)

        val barberCard  = makeCard()
        val barberInner = verticalLayout(0).also {
            it.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }
        barberInner.addView(makeText("Top Barbers by Bookings", 15f, "#1A1A1A", true)
            .also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(12)) })
        val topBarbers = bookings.groupBy { it["barber"] ?: "Unknown" }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }.take(5)
        if (topBarbers.isEmpty()) {
            barberInner.addView(makeText("No booking data yet.", 13f, "#888888")
                .also { it.setPadding(0, dpToPx(8), 0, 0) })
        } else {
            topBarbers.forEachIndexed { i, (name, count) ->
                val row = makeHorizontalRow(dpToPx(6))
                row.addView(makeText("${i + 1}.", 13f, "#888888").also {
                    it.layoutParams = LinearLayout.LayoutParams(dpToPx(24), LinearLayout.LayoutParams.WRAP_CONTENT)
                })
                row.addView(makeText(name, 13f, "#1A1A1A", false).also {
                    it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(makeText("$count bookings", 12f, "#4A90E2"))
                barberInner.addView(row)
                if (i < topBarbers.size - 1) barberInner.addView(makeDivider())
            }
        }
        barberCard.addView(barberInner)
        contentArea.addView(barberCard)

        contentArea.addView(buildTopProducts(orders))
    }

    private val quantityRegex = Regex("""\s+x(\d+)$""")

    private fun buildTopProducts(orders: List<MutableMap<String, String>>): CardView {
        val card  = makeCard()
        val inner = verticalLayout(0).also {
            it.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }
        inner.addView(makeText("Top Products", 15f, "#1A1A1A", true)
            .also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(12)) })

        val productCounts = mutableMapOf<String, Int>()
        orders.forEach { o ->
            val items = (o["items"] ?: o["product"] ?: o["name"] ?: "").trim()
            if (items.isNotBlank()) {
                val parts = if (items.contains("|"))
                    items.split(Regex("\\|"))
                else
                    items.split(",")
                parts.map { it.trim() }.filter { it.isNotBlank() }.forEach { entry ->
                    val quantityMatch = quantityRegex.find(entry)
                    val qty     = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    val product = if (quantityMatch != null)
                        entry.substring(0, quantityMatch.range.first).trim()
                    else
                        entry.trim()
                    if (product.isNotBlank()) {
                        productCounts[product] = (productCounts[product] ?: 0) + qty
                    }
                }
            }
        }

        val topProducts = productCounts.entries
            .sortedByDescending { it.value }
            .take(5)

        if (topProducts.isEmpty()) {
            inner.addView(makeText("No product order data yet.", 13f, "#888888")
                .also { it.setPadding(0, dpToPx(8), 0, 0) })
        } else {
            topProducts.forEachIndexed { i, (name, count) ->
                val row = makeHorizontalRow(dpToPx(8))

                row.addView(makeText("${i + 1}.", 13f, "#888888").also {
                    it.layoutParams = LinearLayout.LayoutParams(dpToPx(24), LinearLayout.LayoutParams.WRAP_CONTENT)
                })

                row.addView(makeText(name, 13f, "#1A1A1A", false).also {
                    it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    (it as TextView).maxLines = 1
                    it.ellipsize = android.text.TextUtils.TruncateAt.END
                })

                row.addView(makeText("$count orders", 12f, "#F5A623", true))

                inner.addView(row)
                if (i < topProducts.size - 1) inner.addView(makeDivider())
            }
        }

        card.addView(inner)
        return card
    }

    private fun cal() = Calendar.getInstance()

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.endOfDay() {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59);      set(Calendar.MILLISECOND, 999)
    }

    private fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(dateStr.split(" ")[0])
        } catch (_: Exception) { null }
    }

}
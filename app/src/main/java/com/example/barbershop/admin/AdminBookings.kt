package com.example.barbershop.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.barbershop.util.AppData
import com.example.barbershop.admin.BaseAdminActivity
import com.example.barbershop.R

class AdminBookings : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bookings)
        setupBase(R.id.btnAdminBookings)
        loadContent()
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Manage Bookings"))

        val filterRow = makeHorizontalRow(dpToPx(12))
        val searchBox = EditText(this).apply {
            hint = "Search by customer..."; textSize = 13f
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            background = roundedBg("#FFFFFF", strokeColor = "#DDDDDD")
            val lp = LinearLayout.LayoutParams(0, dpToPx(44), 1f); lp.setMargins(0, 0, dpToPx(8), 0); layoutParams = lp
        }
        val statusSpinner = Spinner(this)
        val statuses = arrayOf("All Status", "Pending", "Confirmed", "Completed", "Cancelled")
        statusSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        statusSpinner.layoutParams = LinearLayout.LayoutParams(dpToPx(130), dpToPx(44))
        filterRow.addView(searchBox); filterRow.addView(statusSpinner)
        contentArea.addView(filterRow)

        val tableCard = makeCard()
        val tableInner = verticalLayout(dpToPx(12))

        fun rebuildTable(text: String, status: String) {
            tableInner.removeAllViews()
            val filtered = AppData.bookingsList.filter {
                val matchName = it["username"]?.contains(text, ignoreCase = true) ?: false
                val matchStatus = status.isEmpty() || status == "All Status" || it["status"] == status
                matchName && matchStatus
            }
            if (filtered.isEmpty()) {
                tableInner.addView(makeText("No bookings found.", 13f, "#888888").also { it.setPadding(0, dpToPx(16), 0, 0) })
            } else if (isSmallScreen) {
                filtered.forEach { tableInner.addView(makeBookingCard(it, tableInner, searchBox, statusSpinner, statuses)) }
            } else {
                tableInner.addView(makeBookingTableHeader())
                filtered.forEach { tableInner.addView(makeBookingTableRow(it, tableInner, searchBox, statusSpinner, statuses)) }
            }
        }
        rebuildTable("", "")

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = rebuildTable(s.toString(), statusSpinner.selectedItem.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) =
                rebuildTable(searchBox.text.toString(), statuses[pos])
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        tableCard.addView(tableInner)
        contentArea.addView(tableCard)
    }

    private fun showScreenshotDialog(screenshotPath: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setBackgroundColor(Color.WHITE)
        }

        layout.addView(TextView(this).apply {
            text = "Payment Screenshot"
            textSize = 16f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A")); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .also { it.setMargins(0, 0, 0, dpToPx(12)) }
        })

        if (screenshotPath.isNotEmpty()) {
            try {
                val bytes = android.util.Base64.decode(screenshotPath, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                layout.addView(ImageView(this).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(320))
                        .also { it.setMargins(0, 0, 0, dpToPx(12)) }
                })
            } catch (e: Exception) {
                layout.addView(TextView(this).apply {
                    text = "Could not load image."
                    textSize = 13f
                    setTextColor(Color.parseColor("#E53935"))
                    gravity = Gravity.CENTER
                })
            }
        } else {
            layout.addView(TextView(this).apply {
                text = "No screenshot uploaded for this booking."
                textSize = 13f; setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER; setPadding(0, dpToPx(24), 0, dpToPx(24))
            })
        }

        val dialog = Dialog(this)
        layout.addView(Button(this).apply {
            text = "Close"; textSize = 14f
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1C3A52"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44))
            setOnClickListener { dialog.dismiss() }
        })
        dialog.setContentView(layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun makeBookingCard(
        booking: MutableMap<String, String>,
        container: LinearLayout,
        searchBox: EditText,
        statusSpinner: Spinner,
        statuses: Array<String>
    ): CardView {
        val card = makeCard()
        val inner = verticalLayout(0)
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))

        val topRow = makeHorizontalRow(dpToPx(4))
        topRow.addView(makeText("#${booking["id"]} — ${booking["username"]}", 14f, "#1A1A1A", true).also {
            it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        topRow.addView(makeStatusBadge(booking["status"] ?: "Pending"))
        inner.addView(topRow)
        inner.addView(makeText("Barber: ${booking["barber"]}", 12f, "#555555").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 4, 0, 2) })
        inner.addView(makeText("Service: ${booking["service"]}", 12f, "#555555").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, 2) })

        val metaRow = makeHorizontalRow(dpToPx(6))
        metaRow.addView(makeText("${booking["date"]}", 11f, "#888888"))
        metaRow.addView(makeText("   ${booking["time"]}", 11f, "#888888"))
        metaRow.addView(View(this).also { it.layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        metaRow.addView(makeText(booking["amount"] ?: "", 13f, "#4CAF50", true))
        inner.addView(metaRow)

        val screenshotPath = booking["screenshot"] ?: ""
        inner.addView(Button(this).apply {
            setText(if (screenshotPath.isNotEmpty()) "View Payment Screenshot" else "📎 No Screenshot Uploaded")
            textSize = 11f; isEnabled = screenshotPath.isNotEmpty()
            setTextColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#007AFF" else "#AAAAAA"))
            setBackgroundColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#E8F4FF" else "#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(36))
                .also { it.setMargins(0, dpToPx(8), 0, 0) }
            setOnClickListener { showScreenshotDialog(screenshotPath) }
        })

        fun rebuild() { container.removeAllViews(); rebuildAll(container, searchBox, statusSpinner, statuses) }

        val btnRow = makeHorizontalRow(0)
        btnRow.layoutParams = (btnRow.layoutParams as LinearLayout.LayoutParams).also { it.setMargins(0, dpToPx(8), 0, 0) }
        when (booking["status"]) {
            "Pending" -> {
                btnRow.addView(makeActionButton("✓ Confirm", "#4A90E2").also {
                    (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, dpToPx(8), 0)
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Confirmed"); rebuild(); showToast("Booking confirmed!") }
                })
                btnRow.addView(makeActionButton("✕ Cancel", "#E74C3C").also {
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Cancelled"); rebuild(); showToast("Booking cancelled.") }
                })
            }
            "Confirmed" -> {
                btnRow.addView(makeActionButton("✓ Complete", "#4CAF50").also {
                    (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, dpToPx(8), 0)
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Completed"); rebuild(); showToast("Booking completed!") }
                })
                btnRow.addView(makeActionButton("✕ Cancel", "#E74C3C").also {
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Cancelled"); rebuild(); showToast("Booking cancelled.") }
                })
            }
        }
        if (btnRow.childCount > 0) inner.addView(btnRow)
        card.addView(inner)
        return card
    }

    private fun rebuildAll(container: LinearLayout, searchBox: EditText, statusSpinner: Spinner, statuses: Array<String>) {
        val text = searchBox.text.toString()
        val status = statusSpinner.selectedItem.toString()
        val filtered = AppData.bookingsList.filter {
            val matchName = it["username"]?.contains(text, ignoreCase = true) ?: false
            val matchStatus = status.isEmpty() || status == "All Status" || it["status"] == status
            matchName && matchStatus
        }
        if (filtered.isEmpty()) {
            container.addView(makeText("No bookings found.", 13f, "#888888").also { it.setPadding(0, dpToPx(16), 0, 0) })
        } else if (isSmallScreen) {
            filtered.forEach { container.addView(makeBookingCard(it, container, searchBox, statusSpinner, statuses)) }
        } else {
            container.addView(makeBookingTableHeader())
            filtered.forEach { container.addView(makeBookingTableRow(it, container, searchBox, statusSpinner, statuses)) }
        }
    }

    private fun makeBookingTableHeader(): LinearLayout {
        val row = makeHorizontalRow(0).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
        }
        listOf(
            "ID" to 0.35f, "Customer" to 0.65f, "Barber" to 0.8f, "Service" to 0.7f,
            "Date" to 0.7f, "Time" to 0.5f, "Amount" to 0.55f, "Status" to 0.6f,
            "Payment" to 0.55f, "Actions" to 0.9f
        ).forEach { (h, w) ->
            row.addView(makeText(h, 10f, "#666666", true).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            })
        }
        return row
    }

    private fun makeBookingTableRow(
        booking: MutableMap<String, String>,
        container: LinearLayout,
        searchBox: EditText,
        statusSpinner: Spinner,
        statuses: Array<String>
    ): LinearLayout {
        val row = makeHorizontalRow(0).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
            setBackgroundColor(Color.WHITE)
        }
        val amt = booking["amount"] ?: ""
        val amtDisplay = if (amt.startsWith("₱")) amt else "₱$amt"
        listOf(
            "#${booking["id"]}" to 0.35f,
            (booking["username"] ?: "") to 0.65f,
            (booking["barber"] ?: "") to 0.8f,
            (booking["service"] ?: "") to 0.7f,
            (booking["date"] ?: "") to 0.7f,
            (booking["time"] ?: "") to 0.5f,
            amtDisplay to 0.55f
        ).forEach { (text, weight) ->
            row.addView(makeText(text, 10f, "#333333").also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            })
        }
        row.addView(makeStatusBadge(booking["status"] ?: "Pending").also {
            it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
        })

        val screenshotPath = booking["screenshot"] ?: ""
        row.addView(Button(this).apply {
            setText(if (screenshotPath.isNotEmpty()) "View" else "None")
            textSize = 9f; isEnabled = screenshotPath.isNotEmpty()
            setTextColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#007AFF" else "#AAAAAA"))
            setBackgroundColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#E8F4FF" else "#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(28), 0.55f)
            setOnClickListener { showScreenshotDialog(screenshotPath) }
        })

        fun rebuild() { container.removeAllViews(); rebuildAll(container, searchBox, statusSpinner, statuses) }

        val actionCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        when (booking["status"]) {
            "Pending" -> {
                makeActionButton("Confirm", "#4A90E2").also {
                    it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .also { lp -> lp.setMargins(0, 0, 0, dpToPx(4)) }
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Confirmed"); rebuild(); showToast("Confirmed!") }
                    actionCol.addView(it)
                }
                makeActionButton("Cancel", "#E74C3C").also {
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Cancelled"); rebuild(); showToast("Cancelled.") }
                    actionCol.addView(it)
                }
            }
            "Confirmed" -> {
                makeActionButton("Complete", "#4CAF50").also {
                    it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .also { lp -> lp.setMargins(0, 0, 0, dpToPx(4)) }
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Completed"); rebuild(); showToast("Completed!") }
                    actionCol.addView(it)
                }
                makeActionButton("Cancel", "#E74C3C").also {
                    it.setOnClickListener { _ -> persistBookingStatus(booking, "Cancelled"); rebuild(); showToast("Cancelled.") }
                    actionCol.addView(it)
                }
            }
            else -> actionCol.addView(makeText("No actions", 10f, "#AAAAAA"))
        }
        row.addView(actionCol)
        return row
    }
}
package com.example.barbershop.admin

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.barbershop.util.AppData
import com.example.barbershop.admin.BaseAdminActivity
import com.example.barbershop.R

class AdminOrders : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product_orders)
        setupBase(R.id.btnAdminOrders)
        loadContent()
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Product Orders"))

        val filterRow = makeHorizontalRow(dpToPx(12))
        val searchBox = EditText(this).apply {
            hint = "Search order / customer..."; textSize = 13f
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            background = roundedBg("#FFFFFF", strokeColor = "#DDDDDD")
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f).also { it.setMargins(0,0,dpToPx(8),0) }
        }
        val statusSpin = Spinner(this)
        val statuses = arrayOf("All Status","Confirmed","Completed","Cancelled")
        statusSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        statusSpin.layoutParams = LinearLayout.LayoutParams(dpToPx(130), dpToPx(44))
        filterRow.addView(searchBox); filterRow.addView(statusSpin)
        contentArea.addView(filterRow)

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        contentArea.addView(listContainer)

        fun rebuildOrders(text: String, status: String) {
            listContainer.removeAllViews()
            val filtered = AppData.ordersList.filter {
                val matchText   = it["id"]?.contains(text, ignoreCase = true) == true || it["customer"]?.contains(text, ignoreCase = true) == true
                val matchStatus = status.isEmpty() || status == "All Status" || it["status"] == status
                matchText && matchStatus
            }
            if (filtered.isEmpty()) { listContainer.addView(makeText("No orders found.", 13f, "#888888").also { it.setPadding(0,dpToPx(16),0,0) }); return }

            if (isSmallScreen) {
                filtered.forEach { order ->
                    val card  = makeCard()
                    val inner = verticalLayout(0)
                    inner.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
                    val topRow = makeHorizontalRow(dpToPx(4))
                    topRow.addView(makeText("#${order["id"]}", 14f, "#1A1A1A", true).also {
                        it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    topRow.addView(makeStatusBadge(order["status"] ?: "Confirmed"))
                    inner.addView(topRow)
                    inner.addView(makeText("Customer: ${order["customer"]}", 12f, "#333333").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,4,0,2) })
                    inner.addView(makeText(order["date"] ?: "", 11f, "#888888").also                { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,0,0,4) })
                    inner.addView(makeText(order["items"] ?: "", 11f, "#555555").also               { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,0,0,6) })
                    inner.addView(makeText("Total: ${order["total"]}", 14f, "#1A1A1A", true))

                    val screenshotPath = order["screenshot"] ?: ""
                    inner.addView(Button(this).apply {
                        setText(if (screenshotPath.isNotEmpty()) "View Payment Screenshot" else "📎 No Screenshot Uploaded")
                        textSize = 11f; isEnabled = screenshotPath.isNotEmpty()
                        setTextColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#007AFF" else "#AAAAAA"))
                        setBackgroundColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#E8F4FF" else "#F5F5F5"))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(36))
                            .also { it.setMargins(0, dpToPx(8), 0, 0) }
                        setOnClickListener { showScreenshotDialog(screenshotPath) }
                    })

                    if (order["status"] == "Confirmed") {
                        val btnRow = makeHorizontalRow(0)
                        btnRow.layoutParams = (btnRow.layoutParams as LinearLayout.LayoutParams).also { it.setMargins(0, dpToPx(8), 0, 0) }
                        btnRow.addView(makeActionButton("✓ Ready", "#F5A623").also {
                            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,0,dpToPx(8),0)
                            it.setOnClickListener { _ ->
                                persistOrderStatus(order, "Completed")
                                rebuildOrders(searchBox.text.toString(), statusSpin.selectedItem.toString())
                                showToast("Order ready!")
                            }
                        })
                        btnRow.addView(makeActionButton("✕ Reject", "#E74C3C").also {
                            it.setOnClickListener { _ ->
                                persistOrderStatus(order, "Cancelled")
                                rebuildOrders(searchBox.text.toString(), statusSpin.selectedItem.toString())
                                showToast("Order rejected.")
                            }
                        })
                        inner.addView(btnRow)
                    }
                    card.addView(inner)
                    listContainer.addView(card)
                }
            } else {
                val tableCard  = makeCard()
                val tableInner = verticalLayout(dpToPx(12))
                val hdr = makeHorizontalRow(0).apply {
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                    setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
                }
                listOf("Order ID" to 0.6f, "Customer" to 0.7f, "Date" to 0.9f, "Items" to 1.4f,
                    "Total" to 0.6f, "Status" to 0.6f, "Payment" to 0.55f, "Actions" to 0.8f).forEach { (h,w) ->
                    hdr.addView(makeText(h, 10f, "#666666", true).also {
                        it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w) })
                }
                tableInner.addView(hdr)
                filtered.forEach { order ->
                    val row = makeHorizontalRow(0).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
                        setBackgroundColor(Color.WHITE)
                    }
                    row.addView(makeText("#${order["id"]}", 11f, "#333333").also            { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) })
                    row.addView(makeText(order["customer"]?:"", 11f, "#333333", true).also  { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f) })
                    row.addView(makeText(order["date"]?:"", 10f, "#666666").also            { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f) })
                    row.addView(makeText(order["items"]?:"", 10f, "#555555").also           { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f) })
                    row.addView(makeText(order["total"]?:"", 11f, "#333333", true).also     { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) })
                    row.addView(makeStatusBadge(order["status"]?:"Confirmed").also          { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) })

                    val screenshotPath = order["screenshot"] ?: ""
                    row.addView(Button(this).apply {
                        setText(if (screenshotPath.isNotEmpty()) "View" else "None")
                        textSize = 9f; isEnabled = screenshotPath.isNotEmpty()
                        setTextColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#007AFF" else "#AAAAAA"))
                        setBackgroundColor(Color.parseColor(if (screenshotPath.isNotEmpty()) "#E8F4FF" else "#F5F5F5"))
                        layoutParams = LinearLayout.LayoutParams(0, dpToPx(28), 0.55f)
                        setOnClickListener { showScreenshotDialog(screenshotPath) }
                    })

                    val actionCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
                    when (order["status"]) {
                        "Confirmed" -> {
                            makeActionButton("✓ Ready", "#F5A623").also {
                                it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { lp -> lp.setMargins(0,0,0,dpToPx(4)) }
                                it.setOnClickListener { _ ->
                                    persistOrderStatus(order, "Completed")
                                    rebuildOrders(searchBox.text.toString(), statusSpin.selectedItem.toString())
                                    showToast("Order ready!")
                                }
                                actionCol.addView(it)
                            }
                            makeActionButton("✕ Reject", "#E74C3C").also {
                                it.setOnClickListener { _ ->
                                    persistOrderStatus(order, "Cancelled")
                                    rebuildOrders(searchBox.text.toString(), statusSpin.selectedItem.toString())
                                    showToast("Order rejected.")
                                }
                                actionCol.addView(it)
                            }
                        }
                        else -> actionCol.addView(makeText("No actions", 10f, "#AAAAAA"))
                    }
                    row.addView(actionCol)
                    tableInner.addView(row); tableInner.addView(makeDivider())
                }
                tableCard.addView(tableInner)
                listContainer.addView(tableCard)
            }
        }

        rebuildOrders("", "")
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = rebuildOrders(s.toString(), statusSpin.selectedItem.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        statusSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = rebuildOrders(searchBox.text.toString(), statuses[pos])
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
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
                val bytes  = android.util.Base64.decode(screenshotPath, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                text = "No screenshot uploaded for this order."
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
}
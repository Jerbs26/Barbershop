package com.example.barbershop.admin

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import com.example.barbershop.R
import com.example.barbershop.util.AppData
import com.example.barbershop.util.Database

class AdminServices : BaseAdminActivity() {

    private val localServices = mutableListOf<MutableMap<String, String>>()
    private val fixedHeaderCount: Int get() = if (isSmallScreen) 2 else 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_services)
        setupBase(R.id.btnAdminServices)
        buildHeaderRow()
    }

    override fun onResume() {
        super.onResume()
        refreshFromDb()
    }

    private fun buildHeaderRow() {
        val headerRow = makeHorizontalRow(dpToPx(16))
        headerRow.addView(
            makePageTitle("Manage Services").also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        makeActionButton("+ Add Service", "#4A90E2").also {
            it.setOnClickListener { _ -> showServiceDialog(null) }
            headerRow.addView(it)
        }
        contentArea.addView(headerRow)
    }

    // Pull latest services from DB, then redraw

    private fun refreshFromDb() {
        Thread {
            try {
                val rows  = Database(this).use { it.getAllServices() }
                val fresh = rows.map { row ->
                    mutableMapOf(
                        "id"       to (row["id"]       ?: ""),
                        "name"     to (row["name"]     ?: ""),
                        "desc"     to (row["desc"]     ?: ""),
                        "price"    to (row["price"]    ?: ""),
                        "duration" to (row["duration"] ?: "60 minutes")
                    )
                }
                runOnUiThread {
                    localServices.clear()
                    localServices.addAll(fresh)

                    synchronized(AppData.servicesList) {
                        AppData.servicesList.clear()
                        AppData.servicesList.addAll(fresh.map { it.toMutableMap() })
                    }

                    renderServicesCards()
                }
            } catch (e: Exception) {
                runOnUiThread { showToast("Failed to load services") }
            }
        }.start()
    }

    private fun renderServicesCards() {
        while (contentArea.childCount > fixedHeaderCount) {
            contentArea.removeViewAt(fixedHeaderCount)
        }

        if (localServices.isEmpty()) {
            contentArea.addView(
                makeText("No services found.", 13f, "#888888").also {
                    it.setPadding(0, dpToPx(16), 0, 0)
                }
            )
            return
        }

        if (isSmallScreen) {
            localServices.forEach { svc -> contentArea.addView(buildServiceCard(svc)) }
        } else {
            contentArea.addView(buildServiceTable())
        }
    }

    private fun buildServiceCard(svc: MutableMap<String, String>): View {
        val card  = makeCard()
        val inner = verticalLayout(0)
        inner.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))

        inner.addView(makeText(svc["name"] ?: "", 15f, "#1A1A1A", true).also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(4))
        })
        inner.addView(makeText(svc["desc"] ?: "", 12f, "#666666").also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(6))
        })

        val metaRow = makeHorizontalRow(dpToPx(10))
        metaRow.addView(makeText("⏱ ${svc["duration"] ?: ""}", 11f, "#888888"))
        metaRow.addView(View(this).also {
            it.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        metaRow.addView(makeText(svc["price"] ?: "", 14f, "#4CAF50", true))
        inner.addView(metaRow)

        val btnRow = makeHorizontalRow(dpToPx(8))
        btnRow.addView(makeActionButton("Edit", "#F5A623").also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, dpToPx(8), 0)
            it.setOnClickListener { _ -> showServiceDialog(svc) }
        })
        btnRow.addView(makeActionButton("Delete", "#E74C3C").also {
            it.setOnClickListener { _ ->
                showDeleteConfirm("service", svc["name"] ?: "") {
                    deleteServiceFromDb(svc["id"] ?: "")
                }
            }
        })
        inner.addView(btnRow)
        card.addView(inner)
        return card
    }

    private fun buildServiceTable(): View {
        val tableCard  = makeCard()
        val tableInner = verticalLayout(dpToPx(12))

        val hdr = makeHorizontalRow(0).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        }
        listOf(
            "Service Name" to 1.1f,
            "Description"  to 2.0f,
            "Duration"     to 0.7f,
            "Price"        to 0.6f,
            "Action"       to 0.8f
        ).forEach { (h, w) ->
            hdr.addView(makeText(h, 11f, "#666666", true).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            })
        }
        tableInner.addView(hdr)

        localServices.forEach { svc ->
            val row = makeHorizontalRow(0).apply {
                setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12))
                setBackgroundColor(Color.WHITE)
            }
            row.addView(makeText(svc["name"]     ?: "", 12f, "#1A1A1A", true).also { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f) })
            row.addView(makeText(svc["desc"]     ?: "", 11f, "#666666").also { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f) })
            row.addView(makeText(svc["duration"] ?: "", 11f, "#333333").also { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f) })
            row.addView(makeText(svc["price"]    ?: "", 11f, "#4CAF50", true).also  { it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) })

            val actionCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
            }
            makeActionButton("Edit", "#F5A623").also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { lp -> lp.setMargins(0, 0, 0, dpToPx(4)) }
                it.setOnClickListener { _ -> showServiceDialog(svc) }
                actionCol.addView(it)
            }
            makeActionButton("Delete", "#E74C3C").also {
                it.setOnClickListener { _ ->
                    showDeleteConfirm("service", svc["name"] ?: "") {
                        deleteServiceFromDb(svc["id"] ?: "")
                    }
                }
                actionCol.addView(it)
            }
            row.addView(actionCol)
            tableInner.addView(row)
            tableInner.addView(makeDivider())
        }

        tableCard.addView(tableInner)
        return tableCard
    }

    private fun showServiceDialog(existing: MutableMap<String, String>?) {
        val isEdit = existing != null

        val layout = verticalLayout(dpToPx(16))
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(4))

        layout.addView(
            makeText(if (isEdit) "Edit Service" else "Add Service", 17f, "#1A1A1A", true).also {
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(16))
            }
        )

        val nameEt   = makeDialogEditText("Service Name", existing?.get("name") ?: "")
        val descEt   = makeDialogEditText("Description",  existing?.get("desc") ?: "")
        val rawPrice = existing?.get("price")?.replace("₱", "")?.trim() ?: ""
        val priceEt  = makeDialogEditText("Price (₱)", rawPrice)

        val durations = arrayOf("30 minutes", "45 minutes", "50 minutes", "60 minutes", "90 minutes")
        val durSpin   = Spinner(this)
        durSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val durIdx = durations.indexOf(existing?.get("duration") ?: "60 minutes")
        durSpin.setSelection(if (durIdx >= 0) durIdx else 3) // safe fallback to "60 minutes"

        listOf(nameEt, descEt, priceEt).forEach { layout.addView(it) }
        layout.addView(
            makeText("Duration", 12f, "#555555").also {
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(4))
            }
        )
        layout.addView(durSpin)

        AlertDialog.Builder(this)
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                // .tag holds the EditText — confirmed from BaseAdminActivity.makeDialogEditText
                val nameVal  = (nameEt.tag  as EditText).text.toString().trim()
                val descVal  = (descEt.tag  as EditText).text.toString().trim()
                val priceVal = (priceEt.tag as EditText).text.toString().trim()
                val durVal   = durSpin.selectedItem?.toString() ?: "60 minutes"

                if (nameVal.isBlank()) { showToast("Service name is required!"); return@setPositiveButton }
                if (priceVal.isBlank()) { showToast("Price is required!"); return@setPositiveButton }

                // Ensure exactly one ₱ prefix
                val formattedPrice = "₱${priceVal.removePrefix("₱").trim()}"

                if (isEdit) {
                    val id = existing!!["id"] ?: ""
                    if (id.isBlank()) { showToast("Cannot update: invalid ID"); return@setPositiveButton }
                    Thread {
                        try {
                            val ok = Database(this).use {
                                it.updateService(id, nameVal, descVal, formattedPrice, durVal)
                            }
                            runOnUiThread {
                                if (ok) { showToast("Service updated!"); refreshFromDb() }
                                else    showToast("Update failed. Please try again.")
                            }
                        } catch (e: Exception) {
                            runOnUiThread { showToast("Error: ${e.message}") }
                        }
                    }.start()
                } else {
                    Thread {
                        try {
                            val newId = Database(this).use {
                                it.insertService(nameVal, descVal, formattedPrice, durVal)
                            }
                            runOnUiThread {
                                if (newId > 0) { showToast("Service added!"); refreshFromDb() }
                                else           showToast("Insert failed. Please try again.")
                            }
                        } catch (e: Exception) {
                            runOnUiThread { showToast("Error: ${e.message}") }
                        }
                    }.start()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteServiceFromDb(id: String) {
        if (id.isBlank()) { showToast("Cannot delete: invalid ID"); return }
        Thread {
            try {
                val ok = Database(this).use { it.deleteService(id) }
                runOnUiThread {
                    if (ok) { showToast("Service deleted!"); refreshFromDb() }
                    else    showToast("Delete failed. Please try again.")
                }
            } catch (e: Exception) {
                runOnUiThread { showToast("Error: ${e.message}") }
            }
        }.start()
    }
}
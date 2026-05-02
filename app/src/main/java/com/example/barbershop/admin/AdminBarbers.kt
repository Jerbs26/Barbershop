package com.example.barbershop.admin

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.barbershop.util.AppData
import com.example.barbershop.admin.BaseAdminActivity
import com.example.barbershop.R

class AdminBarbers : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_barbers)
        setupBase(R.id.btnAdminBarbers)
        loadContent()
    }

    private fun loadContent() {
        val headerRow = makeHorizontalRow(dpToPx(16))
        val title = makePageTitle("Manage Barbers")
        title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        headerRow.addView(title)
        val addBtn = makeActionButton("+ Add Barber", "#4A90E2")
        addBtn.setOnClickListener { showBarberDialog(null) }
        headerRow.addView(addBtn)
        contentArea.addView(headerRow)
        reloadAndRender()
    }

    private fun reloadAndRender() {
        Thread {
            val freshList = mutableListOf<MutableMap<String, String>>()
            try {
                db.getAllBarbers().forEach { row ->
                    freshList.add(mutableMapOf(
                        "id"        to (row["id"]        ?: ""),
                        "name"      to (row["name"]      ?: ""),
                        "specialty" to (row["specialty"] ?: ""),
                        "desc"      to (row["desc"]      ?: ""),
                        "exp"       to (row["exp"]       ?: ""),
                        "status"    to (row["status"]    ?: "Active")
                    ))
                }
                synchronized(AppData.barbersList) {
                    AppData.barbersList.clear()
                    AppData.barbersList.addAll(freshList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread { renderBarberCards(freshList) }
        }.start()
    }

    private fun renderBarberCards(barbers: List<MutableMap<String, String>>) {

        val keepCount = if (isSmallScreen && contentArea.childCount > 0
            && contentArea.getChildAt(0) is Button) 2 else 1
        while (contentArea.childCount > keepCount) {
            contentArea.removeViewAt(contentArea.childCount - 1)
        }

        if (barbers.isEmpty()) {
            val empty = makeText("No barbers found.", 13f, "#888888")
            empty.setPadding(0, dpToPx(16), 0, 0)
            contentArea.addView(empty)
            return
        }

        var rowLayout: LinearLayout? = null
        barbers.forEachIndexed { index, barber ->
            if (index % 2 == 0) {
                rowLayout = makeHorizontalRow(dpToPx(12))
                contentArea.addView(rowLayout)
            }

            val card = CardView(this).apply {
                radius = dpToPx(10).toFloat()
                cardElevation = dpToPx(3).toFloat()
                setCardBackgroundColor(Color.WHITE)
                isClickable = true
                isFocusable = true
            }
            val cardLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (index % 2 == 0) cardLp.setMargins(0, 0, dpToPx(8), 0)
            card.layoutParams = cardLp

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dpToPx(12), dpToPx(14), dpToPx(12), dpToPx(14))
            }

            val initials = barber["name"]!!.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
            val avatarSize = dpToPx(54)
            val avatarLp = LinearLayout.LayoutParams(avatarSize, avatarSize)
            avatarLp.setMargins(0, 0, 0, dpToPx(8))
            val avatar = TextView(this).apply {
                text = initials
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                background = circleBg(avatarColor(index))
                layoutParams = avatarLp
            }
            inner.addView(avatar)

            val nameLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            nameLp.setMargins(0, 0, 0, dpToPx(2))
            val nameView = makeText(barber["name"]!!, 13f, "#1A1A1A", true)
            nameView.gravity = Gravity.CENTER
            nameView.layoutParams = nameLp
            inner.addView(nameView)

            val specLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            specLp.setMargins(0, 0, 0, dpToPx(4))
            val specView = makeText(barber["specialty"]!!, 10f, "#4A90E2", true)
            specView.gravity = Gravity.CENTER
            specView.layoutParams = specLp
            inner.addView(specView)

            val expLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            expLp.setMargins(0, 0, 0, dpToPx(6))
            val expView = makeText(barber["exp"]!!, 10f, "#999999")
            expView.gravity = Gravity.CENTER
            expView.layoutParams = expLp
            inner.addView(expView)

            val badgeLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            badgeLp.setMargins(0, 0, 0, dpToPx(10))
            val badge = makeStatusBadge(barber["status"]!!)
            badge.gravity = Gravity.CENTER
            badge.layoutParams = badgeLp
            inner.addView(badge)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val editBtn = makeActionButton("Edit", "#F5A623")
            val editLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(30))
            editLp.setMargins(0, 0, dpToPx(6), 0)
            editBtn.layoutParams = editLp
            editBtn.setOnClickListener { showBarberDialog(barber) }
            btnRow.addView(editBtn)

            val deleteBtn = makeActionButton("Delete", "#E74C3C")
            deleteBtn.setOnClickListener {
                showDeleteConfirm("barber", barber["name"]!!) {
                    Thread {
                        try { db.deleteBarber(barber["id"]!!) } catch (e: Exception) { e.printStackTrace() }
                        runOnUiThread {
                            showToast("Barber deleted.")
                            reloadAndRender()
                        }
                    }.start()
                }
            }
            btnRow.addView(deleteBtn)

            inner.addView(btnRow)
            card.addView(inner)
            rowLayout?.addView(card)

            if (index == barbers.size - 1 && index % 2 == 0) {
                val spacer = View(this)
                spacer.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                rowLayout?.addView(spacer)
            }
        }
    }

    private fun showBarberDialog(existing: MutableMap<String, String>?) {
        val isEdit = existing != null
        val layout = verticalLayout(dpToPx(20))
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(4))

        val titleLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        titleLp.setMargins(0, 0, 0, dpToPx(16))
        val titleView = makeText(if (isEdit) "Edit Barber" else "Add Barber", 17f, "#1A1A1A", true)
        titleView.layoutParams = titleLp
        layout.addView(titleView)

        val nameEt = makeDialogEditText("Full Name",      existing?.get("name")      ?: "")
        val specEt = makeDialogEditText("Specialization", existing?.get("specialty") ?: "")
        val descEt = makeDialogEditText("Description",    existing?.get("desc")      ?: "")
        val expEt  = makeDialogEditText("Experience",     existing?.get("exp")       ?: "")

        val statusSpin = Spinner(this).apply {
            val opts = arrayOf("Active", "Inactive")
            adapter = ArrayAdapter(this@AdminBarbers, android.R.layout.simple_spinner_item, opts)
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(if (existing?.get("status") == "Inactive") 1 else 0)
        }

        listOf(nameEt, specEt, descEt, expEt).forEach { layout.addView(it) }

        val statusLabel = makeText("Status", 12f, "#555555")
        val statusLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        statusLp.setMargins(0, 0, 0, dpToPx(4))
        statusLabel.layoutParams = statusLp
        layout.addView(statusLabel)
        layout.addView(statusSpin)

        AlertDialog.Builder(this)
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val nameVal   = (nameEt.tag as EditText).text.toString().trim()
                val specVal   = (specEt.tag as EditText).text.toString().trim()
                val descVal   = (descEt.tag as EditText).text.toString().trim()
                val expVal    = (expEt.tag  as EditText).text.toString().trim()
                val statusVal = statusSpin.selectedItem.toString()

                if (nameVal.isBlank()) { showToast("Name is required!"); return@setPositiveButton }

                Thread {
                    try {
                        if (isEdit) {
                            db.updateBarber(existing!!["id"]!!, nameVal, specVal, descVal, expVal, statusVal)
                        } else {
                            db.insertBarber(nameVal, specVal, descVal, expVal, statusVal)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                    runOnUiThread {
                        showToast(if (isEdit) "Barber updated!" else "Barber added!")
                        reloadAndRender()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
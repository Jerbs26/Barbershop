package com.example.barbershop.admin

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.cardview.widget.CardView
import com.example.barbershop.util.AppData
import com.example.barbershop.admin.BaseAdminActivity
import com.example.barbershop.R

class AdminFeedback : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_feedback)
        setupBase(R.id.btnAdminFeedback)
        loadContent()
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Customer Feedback"))

        val filterRow   = makeHorizontalRow(dpToPx(12))
        val barberNames = arrayOf("All Barbers") + AppData.barbersList.map { it["name"]!! }.toTypedArray()
        val barberSpin  = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminFeedback,
                android.R.layout.simple_spinner_item,
                barberNames
            )
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f).also { it.setMargins(0,0,dpToPx(8),0) }
        }
        val ratingOpts = arrayOf("All Ratings","5 Stars","4 Stars","3 Stars","2 Stars","1 Star")
        val ratingSpin = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminFeedback,
                android.R.layout.simple_spinner_item,
                ratingOpts
            )
                .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f)
        }
        filterRow.addView(barberSpin); filterRow.addView(ratingSpin)
        contentArea.addView(filterRow)

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        contentArea.addView(listContainer)

        fun rebuildFeedback(barber: String, ratingFilter: String) {
            listContainer.removeAllViews()
            val ratingNum = when (ratingFilter) { "5 Stars" -> 5; "4 Stars" -> 4; "3 Stars" -> 3; "2 Stars" -> 2; "1 Star" -> 1; else -> -1 }
            val filtered = AppData.feedbackList.filter {
                (barber == "All Barbers" || it["barber"] == barber) &&
                        (ratingNum == -1 || it["rating"]?.toIntOrNull() == ratingNum)
            }
            if (filtered.isEmpty()) { listContainer.addView(makeText("No feedback found.", 13f, "#888888").also { it.setPadding(0,dpToPx(16),0,0) }); return }

            filtered.forEach { fb ->
                val ratingId = fb["id"] ?: ""
                val card  = makeCard()
                val inner = verticalLayout(0)
                inner.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))

                val topRow = makeHorizontalRow(dpToPx(4))
                topRow.addView(makeText(fb["customer"] ?: fb["user"] ?: "", 14f, "#1A1A1A", true).also {
                    it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                val starCount = fb["rating"]?.toIntOrNull() ?: 0
                val stars = "★".repeat(starCount) + "☆".repeat(5 - starCount)
                topRow.addView(makeText(stars, 14f, "#F5A623"))
                inner.addView(topRow)

                inner.addView(makeText("Barber: ${fb["barber"]}",   11f, "#555555").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,4,0,2) })
                inner.addView(makeText("Service: ${fb["service"]}", 11f, "#555555").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,0,0,2) })
                inner.addView(makeText(fb["date"]!!,                10f, "#999999").also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0,0,0,6) })

                val commentBg = CardView(this).apply {
                    radius = dpToPx(6).toFloat(); cardElevation = 0f
                    setCardBackgroundColor(Color.parseColor("#F8F8F8"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .also { it.setMargins(0,0,0,dpToPx(10)) }
                }
                val cInner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dpToPx(10),dpToPx(8),dpToPx(10),dpToPx(8)) }
                cInner.addView(makeText("\"${fb["comment"]}\"", 12f, "#444444"))
                commentBg.addView(cInner)
                inner.addView(commentBg)

                inner.addView(makeActionButton("🗑 Delete", "#E74C3C").also {
                    it.setOnClickListener { _ ->
                        showDeleteConfirm("feedback", "from ${fb["customer"] ?: fb["user"]}") {
                            if (ratingId.isNotEmpty()) {
                                Thread { db.deleteRating(ratingId) }.start()
                            }
                            AppData.feedbackList.remove(fb)
                            rebuildFeedback(barberSpin.selectedItem.toString(), ratingSpin.selectedItem.toString())
                            showToast("Feedback deleted.")
                        }
                    }
                })
                card.addView(inner)
                listContainer.addView(card)
            }
        }

        rebuildFeedback("All Barbers", "All Ratings")
        barberSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = rebuildFeedback(barberNames[pos], ratingSpin.selectedItem.toString())
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        ratingSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = rebuildFeedback(barberSpin.selectedItem.toString(), ratingOpts[pos])
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }
}
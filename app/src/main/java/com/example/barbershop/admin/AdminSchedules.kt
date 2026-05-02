package com.example.barbershop.admin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.barbershop.util.AppData
import com.example.barbershop.util.Database
import com.example.barbershop.R

class NonInterceptHorizontalScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return super.onTouchEvent(ev)
    }
}

class AdminSchedules : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_schedules)
        setupBase(R.id.btnAdminSchedules)
        loadContent()
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Barber Schedules"))

        val card  = makeCard()
        val inner = verticalLayout(dpToPx(16))

        inner.addView(makeText("Select Barber:", 13f, "#333333", true)
            .also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(8)) })

        val barberNames = arrayOf("Choose a barber...") +
                AppData.barbersList.map { it["name"]!! }.toTypedArray()

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminSchedules,
                android.R.layout.simple_spinner_item,
                barberNames
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)
            ).also { it.setMargins(0, 0, 0, dpToPx(16)) }
        }
        inner.addView(spinner)

        val scheduleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        inner.addView(scheduleContainer)

        val hours = listOf(
            "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
            "1:00 PM",  "2:00 PM",  "3:00 PM",  "4:00 PM",
            "5:00 PM",  "6:00 PM"
        )

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                scheduleContainer.removeAllViews()
                if (pos == 0) return

                val barberName = barberNames[pos]

                val db = Database(this@AdminSchedules)
                val restDays = db.getRestDays(barberName).toMutableSet()
                db.close()

                scheduleContainer.addView(
                    makeText("$barberName — Weekly Time Slots", 13f, "#1A1A1A", true)
                        .also { (it.layoutParams as LinearLayout.LayoutParams)
                            .setMargins(0, 0, 0, dpToPx(8)) }
                )
                scheduleContainer.addView(
                    makeText(
                        "Tap a day header to toggle rest day  🔴 = rest day",
                        11f, "#888888", false
                    ).also { (it.layoutParams as LinearLayout.LayoutParams)
                        .setMargins(0, 0, 0, dpToPx(12)) }
                )

                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                val scrollH = NonInterceptHorizontalScrollView(this@AdminSchedules).apply {
                    isHorizontalScrollBarEnabled = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val daysRow = LinearLayout(this@AdminSchedules).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                days.forEach { day ->
                    val colW   = if (isSmallScreen) dpToPx(72) else dpToPx(86)
                    val isRest = restDays.contains(day)

                    val dayCol = LinearLayout(this@AdminSchedules).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dpToPx(2), 0, dpToPx(2), 0)
                        layoutParams = LinearLayout.LayoutParams(
                            colW, LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val dayHeader = Button(this@AdminSchedules).apply {
                        text      = day
                        textSize  = 11f
                        typeface  = Typeface.DEFAULT_BOLD
                        gravity   = Gravity.CENTER
                        isAllCaps = false
                        elevation = 0f
                        stateListAnimator = null
                        setPadding(0, 0, 0, 0)
                        minHeight = 0
                        minimumHeight = 0
                        setBackgroundColor(
                            Color.parseColor(if (isRest) "#D32F2F" else "#CCCCCC")
                        )
                        setTextColor(
                            Color.parseColor(if (isRest) "#FFFFFF" else "#333333")
                        )
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(34)
                        ).also { it.setMargins(0, 0, 0, dpToPx(4)) }
                    }

                    dayHeader.setOnClickListener {
                        val nowRest = restDays.contains(day)
                        val newType = if (nowRest) "available" else "rest"

                        val db2 = Database(this@AdminSchedules)
                        db2.setBarberDayType(barberName, day, newType)
                        db2.close()

                        if (nowRest) restDays.remove(day) else restDays.add(day)
                        val nowIsRest = restDays.contains(day)

                        dayHeader.setBackgroundColor(
                            Color.parseColor(if (nowIsRest) "#D32F2F" else "#CCCCCC")
                        )
                        dayHeader.setTextColor(
                            Color.parseColor(if (nowIsRest) "#FFFFFF" else "#333333")
                        )

                        for (i in 1 until dayCol.childCount) {
                            val slot = dayCol.getChildAt(i) as? TextView ?: continue
                            slot.isEnabled = !nowIsRest
                            slot.setTextColor(
                                Color.parseColor(if (nowIsRest) "#AAAAAA" else "#333333")
                            )
                            slot.background = roundedBg(
                                if (nowIsRest) "#EEEEEE" else "#FFFFFF",
                                strokeColor = if (nowIsRest) "#CCCCCC" else "#DDDDDD"
                            )
                        }

                        val msg = if (nowIsRest)
                            "$day is now a rest day for $barberName"
                        else
                            "$day is now available for $barberName"
                        Toast.makeText(this@AdminSchedules, msg, Toast.LENGTH_SHORT).show()
                    }

                    dayCol.addView(dayHeader)

                    hours.forEach { hour ->
                        dayCol.addView(TextView(this@AdminSchedules).apply {
                            text      = hour
                            textSize  = 9f
                            gravity   = Gravity.CENTER
                            isEnabled = !isRest
                            setPadding(dpToPx(2), dpToPx(5), dpToPx(2), dpToPx(5))
                            setTextColor(
                                Color.parseColor(if (isRest) "#AAAAAA" else "#333333")
                            )
                            background = roundedBg(
                                if (isRest) "#EEEEEE" else "#FFFFFF",
                                strokeColor = if (isRest) "#CCCCCC" else "#DDDDDD"
                            )
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).also { it.setMargins(0, 0, 0, dpToPx(3)) }
                        })
                    }

                    daysRow.addView(dayCol)
                }

                scrollH.addView(daysRow)
                scheduleContainer.addView(scrollH)
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        card.addView(inner)
        contentArea.addView(card)
    }
}
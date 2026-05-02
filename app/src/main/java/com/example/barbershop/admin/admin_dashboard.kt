package com.example.barbershop.admin

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import com.example.barbershop.util.AppData
import com.example.barbershop.R

class AdminDashboard : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        setupBase(R.id.btnAdminDashboard)
    }

    override fun onResume() {
        super.onResume()
        val startIndex = if (isSmallScreen) 1 else 0
        while (contentArea.childCount > startIndex) {
            contentArea.removeViewAt(startIndex)
        }
        loadContent()
    }

    private fun loadContent() {
        contentArea.addView(makePageTitle("Dashboard Overview"))

        val loadingText = makeText("Loading...", 13f, "#888888")
        contentArea.addView(loadingText)

        Thread {
            val freshBookings = mutableListOf<MutableMap<String, String>>()
            val freshBarbers  = mutableListOf<MutableMap<String, String>>()
            val freshOrders   = mutableListOf<MutableMap<String, String>>()
            try {
                db.getAllBookings().forEach { row ->
                    freshBookings.add(mutableMapOf(
                        "id"         to (row["id"]         ?: ""),
                        "username"   to (row["username"]   ?: ""),
                        "email"      to (row["email"]      ?: ""),
                        "barber"     to (row["barber"]     ?: ""),
                        "service"    to (row["service"]    ?: ""),
                        "date"       to (row["date"]       ?: ""),
                        "time"       to (row["time"]       ?: ""),
                        "amount"     to (row["amount"]     ?: ""),
                        "status"     to (row["status"]     ?: "Pending"),
                        "screenshot" to (row["screenshot"] ?: "")
                    ))
                }
                db.getAllBarbers().forEach { row ->
                    freshBarbers.add(mutableMapOf(
                        "id"        to (row["id"]        ?: ""),
                        "name"      to (row["name"]      ?: ""),
                        "specialty" to (row["specialty"] ?: ""),
                        "desc"      to (row["desc"]      ?: ""),
                        "exp"       to (row["exp"]       ?: ""),
                        "status"    to (row["status"]    ?: "Active")
                    ))
                }
                db.getAllOrders().forEach { row ->
                    freshOrders.add(mutableMapOf(
                        "id"     to (row["id"]     ?: ""),
                        "total"  to (row["total"]  ?: ""),
                        "status" to (row["status"] ?: "Confirmed")
                    ))
                }
                synchronized(AppData.bookingsList) {
                    AppData.bookingsList.clear()
                    AppData.bookingsList.addAll(freshBookings)
                }
                synchronized(AppData.barbersList) {
                    AppData.barbersList.clear()
                    AppData.barbersList.addAll(freshBarbers)
                }
                synchronized(AppData.ordersList) {
                    AppData.ordersList.clear()
                    AppData.ordersList.addAll(freshOrders)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    contentArea.removeView(loadingText)
                    renderStats(freshBookings, freshBarbers, freshOrders)
                }
            }
        }.start()
    }

    private fun renderStats(
        bookings: List<MutableMap<String, String>>,
        barbers:  List<MutableMap<String, String>>,
        orders:   List<MutableMap<String, String>>
    ) {
        val pending = bookings.count { it["status"] == "Pending" }
        val active  = barbers.count  { it["status"] == "Active" }

        val bookingRevenue = bookings
            .filter { it["status"] == "Completed" }
            .sumOf { parsePeso(it["amount"]) }
        val orderRevenue = orders
            .filter { it["status"] == "Completed" }
            .sumOf { parsePeso(it["total"]) }
        val totalRevenue = bookingRevenue + orderRevenue

        if (isSmallScreen) {
            val row1 = makeHorizontalRow(dpToPx(10))
            row1.addView(makeStatCard("Total Bookings", bookings.size.toString(), "#4A90E2", 1f))
            row1.addView(makeStatCard("Pending",        pending.toString(),       "#F5A623", 1f))
            contentArea.addView(row1)

            val row2 = makeHorizontalRow(dpToPx(16))
            row2.addView(makeStatCard("Active Barbers", active.toString(),                          "#7E7E9A", 1f))
            row2.addView(makeStatCard("Revenue",        "₱${String.format("%.2f", totalRevenue)}", "#4CAF50", 1f))
            contentArea.addView(row2)
        } else {
            val statsRow = makeHorizontalRow(dpToPx(16))
            statsRow.addView(makeStatCard("Total Bookings",   bookings.size.toString(),                 "#4A90E2", 1f))
            statsRow.addView(makeStatCard("Pending Bookings", pending.toString(),                        "#F5A623", 1f))
            statsRow.addView(makeStatCard("Active Barbers",   active.toString(),                         "#7E7E9A", 1f))
            statsRow.addView(makeStatCard("Total Revenue",    "₱${String.format("%.2f", totalRevenue)}", "#4CAF50", 1f))
            contentArea.addView(statsRow)
        }

        val tableCard  = makeCard()
        val tableInner = verticalLayout(dpToPx(16))
        tableInner.addView(
            makeText("Recent Bookings", 16f, "#1A1A1A", true).also {
                (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, dpToPx(12))
            }
        )

        if (bookings.isEmpty()) {
            tableInner.addView(
                makeText("No bookings yet.", 13f, "#888888").also {
                    (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dpToPx(8), 0, 0)
                }
            )
        } else if (isSmallScreen) {
            bookings.take(7).forEach { tableInner.addView(makeBookingCard(it)) }
        } else {
            tableInner.addView(makeBookingTableHeader())
            bookings.take(7).forEach { tableInner.addView(makeBookingTableRow(it)) }
        }

        tableCard.addView(tableInner)
        contentArea.addView(tableCard)
    }

    private fun resolveDisplayName(booking: MutableMap<String, String>): String {
        return booking["username"]?.takeIf { it.isNotBlank() }
            ?: booking["email"]?.takeIf { it.isNotBlank() }
            ?: "Unknown"
    }

    private fun makeBookingCard(booking: MutableMap<String, String>): CardView {
        val card  = makeCard()
        val inner = verticalLayout(0)
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))

        val topRow = makeHorizontalRow(dpToPx(4))
        topRow.addView(
            makeText("#${booking["id"]} — ${resolveDisplayName(booking)}", 14f, "#1A1A1A", true).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        topRow.addView(makeStatusBadge(booking["status"] ?: "Pending"))
        inner.addView(topRow)

        inner.addView(makeText("Barber: ${booking["barber"] ?: ""}", 12f, "#555555").also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 4, 0, 2)
        })
        inner.addView(makeText("Service: ${booking["service"] ?: ""}", 12f, "#555555").also {
            (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 0, 2)
        })

        val metaRow = makeHorizontalRow(dpToPx(6))
        metaRow.addView(makeText(booking["date"] ?: "", 11f, "#888888"))
        metaRow.addView(makeText("   ${booking["time"] ?: ""}", 11f, "#888888"))
        metaRow.addView(View(this).also {
            it.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })
        metaRow.addView(makeText(booking["amount"] ?: "", 13f, "#4CAF50", true))
        inner.addView(metaRow)

        card.addView(inner)
        return card
    }

    private fun makeBookingTableHeader(): LinearLayout {
        val row = makeHorizontalRow(0).apply {
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
        }
        listOf(
            "ID"       to 0.35f,
            "Customer" to 0.65f,
            "Barber"   to 0.8f,
            "Service"  to 0.7f,
            "Date"     to 0.7f,
            "Time"     to 0.5f,
            "Amount"   to 0.55f,
            "Status"   to 0.6f
        ).forEach { (h, w) ->
            row.addView(makeText(h, 10f, "#666666", true).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            })
        }
        return row
    }

    private fun makeBookingTableRow(booking: MutableMap<String, String>): LinearLayout {
        val row = makeHorizontalRow(0).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))
            setBackgroundColor(Color.WHITE)
        }

        val amt        = booking["amount"] ?: ""
        val amtDisplay = if (amt.startsWith("₱")) amt else "₱$amt"

        listOf(
            "#${booking["id"] ?: ""}"   to 0.35f,
            resolveDisplayName(booking) to 0.65f,
            (booking["barber"]  ?: "")  to 0.8f,
            (booking["service"] ?: "")  to 0.7f,
            (booking["date"]    ?: "")  to 0.7f,
            (booking["time"]    ?: "")  to 0.5f,
            amtDisplay                  to 0.55f
        ).forEach { (text, weight) ->
            row.addView(makeText(text, 10f, "#333333").also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            })
        }

        row.addView(makeStatusBadge(booking["status"] ?: "Pending").also {
            it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
        })
        return row
    }
}
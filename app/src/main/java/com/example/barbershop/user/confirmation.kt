package com.example.barbershop.user

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.R
import com.example.barbershop.util.Database

class confirmation : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmation)

        // Pull all booking details passed from the Dashboard/GCash flow
        val email          = intent.getStringExtra("email")          ?: ""
        val username       = intent.getStringExtra("username")       ?: "—"
        val barber         = intent.getStringExtra("barber")         ?: "—"
        val service        = intent.getStringExtra("service")        ?: "—"
        val date           = intent.getStringExtra("date")           ?: "—"
        val timeSlot       = intent.getStringExtra("timeSlot")       ?: "—"
        val totalAmount    = intent.getStringExtra("totalAmount")    ?: "₱0.00"
        val screenshotPath = intent.getStringExtra("screenshotPath") ?: ""

        // Populate the confirmation UI fields
        findViewById<TextView>(R.id.tvName).text        = username
        findViewById<TextView>(R.id.tvBarber).text      = barber
        findViewById<TextView>(R.id.tvService).text     = service
        findViewById<TextView>(R.id.tvDate).text        = date
        findViewById<TextView>(R.id.tvTimeSlot).text    = timeSlot
        findViewById<TextView>(R.id.tvTotalAmount).text = totalAmount

        // Save booking to database — including screenshot path
        val db = Database(this)
        db.saveBooking(
            userEmail      = email,
            barber         = barber,
            service        = service,
            date           = date,
            timeSlot       = timeSlot,
            totalAmount    = totalAmount,
            screenshotPath = screenshotPath
        )

        // Send the user back to the home dashboard after viewing confirmation
        findViewById<Button>(R.id.backToHomeButton).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java).apply {
                putExtra("email", email)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }
}
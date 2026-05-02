package com.example.barbershop.user

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.R
import com.example.barbershop.util.Database

class OrderConfirmation : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        // Get order details passed from the Cart screen
        val email          = intent.getStringExtra("email")          ?: ""
        val items          = intent.getStringExtra("items")          ?: "—"
        val totalAmount    = intent.getStringExtra("totalAmount")    ?: "₱0.00"
        val screenshotPath = intent.getStringExtra("screenshotPath") ?: ""

        // Generate a random-looking order ID for display purposes
        val orderId = "#ORD-${(10000000..99999999).random()}"

        // Show the order summary to the user
        findViewById<TextView>(R.id.tvOrderId).text    = orderId
        findViewById<TextView>(R.id.tvOrderItems).text = items
        findViewById<TextView>(R.id.tvOrderTotal).text = totalAmount

        // Save the order to the database including the payment screenshot
        val db = Database(this)
        db.saveOrder(
            userEmail      = email,
            items          = items,
            totalAmount    = totalAmount,
            screenshotPath = screenshotPath
        )

        // Take the user back to the dashboard when they're done
        findViewById<Button>(R.id.backToHomeButton).setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java).apply {
                putExtra("email", email)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }
}
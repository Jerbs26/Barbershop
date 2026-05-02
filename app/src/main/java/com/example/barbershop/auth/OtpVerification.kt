package com.example.barbershop.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.Database
import com.example.barbershop.R

class OtpVerification : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        // Grab what was passed from the SignUp screen
        val correctOtp = intent.getStringExtra("otp") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val password = intent.getStringExtra("password") ?: ""
        val name = intent.getStringExtra("name") ?: ""

        val otpInput = findViewById<EditText>(R.id.otpInput)
        val verifyButton = findViewById<Button>(R.id.verifyOtpButton)
        val otpSubtitle = findViewById<TextView>(R.id.otpSubtitle)
        val closeButton = findViewById<ImageButton>(R.id.closeButton)

        val db = Database(this)

        // Show the user which email we sent the OTP to
        otpSubtitle.text = "We've sent a 6-digit OTP code to $email\nEnter the code to verify."

        verifyButton.setOnClickListener {
            val enteredOtp = otpInput.text.toString().trim()

            if (enteredOtp == correctOtp) {
                // OTP matched — try to register the user in the database
                val success = db.registerUser(email, password, name)
                if (success) {
                    Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show()
                } else {
                    // Email already exists, just tell them to log in instead
                    Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_SHORT).show()
                }
                // Either way, go to login
                startActivity(Intent(this, logIn::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid OTP. Try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Just close the screen, no action taken
        closeButton.setOnClickListener {
            finish()
        }
    }
}
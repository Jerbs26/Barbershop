package com.example.barbershop.user

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.R
import com.example.barbershop.auth.ForgotPassword
import com.example.barbershop.auth.ResetPassword

class VerifyOTP : AppCompatActivity() {

    private lateinit var otpInput: EditText
    private lateinit var verifyOtpButton: Button
    private lateinit var resendText: TextView
    private lateinit var otpExpiryBanner: TextView
    private var receivedOtp: String = ""
    private var userEmail: String   = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        // The OTP and email were passed from ForgotPassword after sending the email
        receivedOtp = intent.getStringExtra("otp")   ?: ""
        userEmail   = intent.getStringExtra("email") ?: ""

        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        otpInput        = findViewById(R.id.otpInput)
        verifyOtpButton = findViewById(R.id.verifyOtpButton)
        resendText      = findViewById(R.id.resendText)
        otpExpiryBanner = findViewById(R.id.otpExpiryBanner)

        // Start the countdown immediately so the user sees the timer right away
        startTimer()

        closeButton.setOnClickListener { finish() }

        verifyOtpButton.setOnClickListener {
            val enteredOtp = otpInput.text.toString().trim()

            when {
                enteredOtp.isEmpty() -> {
                    Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
                }
                enteredOtp.length < 6 -> {
                    Toast.makeText(this, "Please enter the complete 6-digit OTP", Toast.LENGTH_SHORT).show()
                }
                receivedOtp.isEmpty() -> {
                    // OTP was invalidated when the timer expired
                    Toast.makeText(this, "OTP has expired. Please request a new one.", Toast.LENGTH_SHORT).show()
                }
                enteredOtp != receivedOtp -> {
                    Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Correct OTP — stop the timer and move to password reset
                    countDownTimer?.cancel()
                    Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ResetPassword::class.java).apply {
                        putExtra("email", userEmail)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    // 10-minute countdown — updates the banner each second and handles expiry
    private fun startTimer() {
        // Disable resend until the timer finishes
        resendText.isEnabled = false
        resendText.alpha     = 0.5f

        countDownTimer = object : CountDownTimer(600000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                otpExpiryBanner.text = "OTP Code expires in %02d:%02d minutes".format(minutes, seconds)
                resendText.text      = "Resend available in %02d minutes.".format(minutes)
            }

            override fun onFinish() {
                otpExpiryBanner.text = "OTP has expired. Please request a new one."
                resendText.text      = "Resend OTP"
                resendText.isEnabled = true
                resendText.alpha     = 1.0f

                // Blank out the stored OTP so it can't be used anymore
                receivedOtp = ""

                // Tapping resend sends them back to ForgotPassword to get a fresh code
                resendText.setOnClickListener {
                    startActivity(Intent(this@VerifyOTP, ForgotPassword::class.java))
                    finish()
                }
            }
        }.start()
    }

    // Always cancel the timer when the screen is destroyed to avoid memory leaks
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
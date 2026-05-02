package com.example.barbershop.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.Database
import com.example.barbershop.R
import com.example.barbershop.user.VerifyOTP
import kotlinx.coroutines.*
import java.util.*
import javax.mail.*
import javax.mail.internet.*

class ForgotPassword : AppCompatActivity() {

    private lateinit var db: Database

    // Gmail credentials used for sending OTP emails
    private val senderEmail = "ardientejerby26@gmail.com"
    private val senderPassword = "yvzupxqjuonbthex"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        db = Database(this)

        // Hook up all the UI elements from the layout
        val closeButton         = findViewById<ImageButton>(R.id.closeButton)
        val sendResetCodeButton = findViewById<Button>(R.id.sendResetCodeButton)
        val backToLoginButton   = findViewById<Button>(R.id.backToLoginButton)
        val emailInput          = findViewById<EditText>(R.id.emailInput)

        // Just close this screen when the X is tapped
        closeButton.setOnClickListener { finish() }

        // Go back to the login screen if the user remembers their password
        backToLoginButton.setOnClickListener {
            startActivity(Intent(this, logIn::class.java))
            finish()
        }

        sendResetCodeButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            // Don't let them proceed with an empty field
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Basic format check before hitting the database
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Make sure the email actually belongs to a registered user
            if (!db.emailExists(email)) {
                Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // All checks passed — generate and send the OTP
            val otp = generateOTP()
            sendOTPEmail(email, otp)
        }
    }

    // Generates a random 6-digit OTP code for password reset
    private fun generateOTP(): String = (100000..999999).random().toString()

    // Sends the OTP to the user's email using Gmail SMTP, then navigates to the verification screen
    private fun sendOTPEmail(recipientEmail: String, otp: String) {
        // Run this on a background thread so we don't block the UI while sending
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Set up SMTP properties for Gmail with TLS
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.ssl.trust", "smtp.gmail.com")
                }

                // Authenticate with our sender Gmail account
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(senderEmail, senderPassword)
                })

                // Build the email message with the OTP inside
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail, "Urban Razor"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    subject = "Your Password Reset OTP"
                    setText("""
                        Hello,

                        Your OTP code for password reset is:

                        $otp

                        This code expires in 10 minutes.

                        If you did not request this, please ignore this email.

                        - Urban Razor Team
                    """.trimIndent())
                }

                Transport.send(message)

                // Back on the main thread — show feedback and move to OTP verification
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPassword, "OTP sent to $recipientEmail", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@ForgotPassword, VerifyOTP::class.java).apply {
                        // Pass the OTP and email so the next screen can verify the code
                        putExtra("otp", otp)
                        putExtra("email", recipientEmail)
                    }
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                // Something went wrong with the email — show the error to the user
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPassword, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
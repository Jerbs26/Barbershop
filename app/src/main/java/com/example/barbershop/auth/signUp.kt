package com.example.barbershop.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.Database
import com.example.barbershop.util.OtpEmailSender
import com.example.barbershop.auth.OtpVerification
import com.example.barbershop.R
import kotlinx.coroutines.*
class SignUp : AppCompatActivity() {

    private var generatedOtp: String = ""
    private var isPasswordVisible    = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val closeButton         = findViewById<ImageButton>(R.id.closeButton)
        val signUpButton        = findViewById<Button>(R.id.signUpButton)
        val backToLogin         = findViewById<Button>(R.id.backToLoginButton)
        val emailInput          = findViewById<EditText>(R.id.emailInput)
        val passwordInput       = findViewById<EditText>(R.id.passwordInput)
        val btnTogglePassword   = findViewById<ImageButton>(R.id.btnTogglePassword)

        val db = Database(this)

        // Toggle show/hide password when the eye icon is tapped
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.transformationMethod = null
                btnTogglePassword.setImageResource(android.R.drawable.ic_secure)
            } else {
                passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
            }
            // Keep cursor at the end so it doesn't jump back
            passwordInput.setSelection(passwordInput.text.length)
        }

        closeButton.setOnClickListener { finish() }

        // Go back to login if they already have an account
        backToLogin.setOnClickListener {
            startActivity(Intent(this, logIn::class.java))
            finish()
        }

        signUpButton.setOnClickListener {
            val email    = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Basic validation before we do anything else
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Check the database first — no point sending an OTP if the email is already taken
            if (db.emailExists(email)) {
                Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Generate a fresh OTP and send it in the background
            generatedOtp = OtpEmailSender.generateOtp()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    OtpEmailSender.sendOtp(email, generatedOtp)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignUp, "OTP sent to $email", Toast.LENGTH_SHORT).show()

                        // Pass everything to OtpVerification so it can finish the registration
                        startActivity(Intent(this@SignUp, OtpVerification::class.java).apply {
                            putExtra("otp",      generatedOtp)
                            putExtra("email",    email)
                            putExtra("password", password)
                            putExtra("name",     email.substringBefore("@"))
                        })
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignUp, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
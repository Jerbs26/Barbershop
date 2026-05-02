package com.example.barbershop.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.BiometricHelper
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.util.Database
import com.example.barbershop.R
import com.example.barbershop.admin.AdminDashboard
import com.example.barbershop.user.Dashboard

class logIn : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.resetToLight()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        sharedPreferences = getSharedPreferences("RememberMe", MODE_PRIVATE)

        val closeButton          = findViewById<ImageButton>(R.id.closeButton)
        val loginButton          = findViewById<Button>(R.id.loginButton)
        val signUpButton         = findViewById<Button>(R.id.signUpText)
        val forgotPasswordButton = findViewById<Button>(R.id.forgotPasswordText)
        val emailInput           = findViewById<EditText>(R.id.emailInput)
        val passwordInput        = findViewById<EditText>(R.id.passwordInput)
        val rememberMeCheckbox   = findViewById<CheckBox>(R.id.rememberMeCheckbox)
        val biometricButton      = findViewById<Button>(R.id.biometricButton)

        val db = Database(this)

        if (sharedPreferences.getBoolean("remember", false)) {
            emailInput.setText(sharedPreferences.getString("email", ""))
            passwordInput.setText(sharedPreferences.getString("password", ""))
            rememberMeCheckbox.isChecked = true
        }

        val biometricPrefs   = getSharedPreferences("BiometricPrefs", MODE_PRIVATE)
        val lastEmail        = biometricPrefs.getString("LAST_EMAIL", null)
        val biometricEnabled = biometricPrefs.getBoolean("BIOMETRIC_ENABLED", false)

        if (lastEmail != null && biometricEnabled && BiometricHelper.isAvailable(this)) {
            biometricButton.visibility = View.VISIBLE
            biometricButton.setOnClickListener {
                BiometricHelper.showPrompt(
                    activity  = this,
                    onSuccess = {
                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("USER_EMAIL", lastEmail)
                            .apply()

                        DarkModeHelper.applyForUser(this, lastEmail)

                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Dashboard::class.java).apply {
                            putExtra("email", lastEmail)
                        })
                        finish()
                    },
                    onError = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            biometricButton.visibility = View.GONE
        }

        closeButton.setOnClickListener { finish() }

        passwordInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = passwordInput.compoundDrawables[2]
                if (drawableEnd != null &&
                    event.rawX >= (passwordInput.right - drawableEnd.bounds.width() - passwordInput.paddingEnd)) {
                    isPasswordVisible = !isPasswordVisible
                    passwordInput.transformationMethod =
                        if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
                    passwordInput.setSelection(passwordInput.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        loginButton.setOnClickListener {
            val email    = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email == "admin001" && password == "password") {
                getSharedPreferences("AdminPrefs", MODE_PRIVATE).edit()
                    .putBoolean("IS_ADMIN", true)
                    .putString("ADMIN_USERNAME", email)
                    .apply()
                DarkModeHelper.resetToLight()
                Toast.makeText(this, "Welcome, Admin!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboard::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
                return@setOnClickListener
            }

            if (db.loginUser(email, password)) {

                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                    .putString("USER_EMAIL", email)
                    .apply()

                if (BiometricHelper.isAvailable(this)) {
                    biometricPrefs.edit()
                        .putString("LAST_EMAIL", email)
                        .putBoolean("BIOMETRIC_ENABLED", true)
                        .apply()
                }

                DarkModeHelper.applyForUser(this, email)

                val editor = sharedPreferences.edit()
                if (rememberMeCheckbox.isChecked) {
                    editor.putBoolean("remember", true)
                    editor.putString("email", email)
                    editor.putString("password", password)
                } else {
                    editor.clear()
                }
                editor.apply()

                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Dashboard::class.java).apply {
                    putExtra("email", email)
                })
                finish()

            } else {
                Toast.makeText(this, "No account found. Please sign up first.", Toast.LENGTH_LONG).show()
            }
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }
    }
}
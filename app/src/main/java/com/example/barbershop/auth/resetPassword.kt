package com.example.barbershop.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.Database
import com.example.barbershop.R

class ResetPassword : AppCompatActivity() {

    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var db: Database
    private var userEmail: String = ""
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        db = Database(this)
        // Pull the email that was passed along from the previous screen
        userEmail = intent.getStringExtra("email") ?: ""

        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)

        // Close this screen without making any changes
        closeButton.setOnClickListener { finish() }

        // Toggle visibility for the new password field when the eye icon is tapped
        newPasswordInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = newPasswordInput.compoundDrawables[2]
                if (drawableEnd != null &&
                    event.rawX >= (newPasswordInput.right - drawableEnd.bounds.width() - newPasswordInput.paddingEnd)) {
                    isNewPasswordVisible = !isNewPasswordVisible
                    newPasswordInput.transformationMethod =
                        if (isNewPasswordVisible) null else PasswordTransformationMethod.getInstance()
                    // Keep the cursor at the end so it doesn't jump around
                    newPasswordInput.setSelection(newPasswordInput.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Same toggle logic for the confirm password field
        confirmPasswordInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = confirmPasswordInput.compoundDrawables[2]
                if (drawableEnd != null &&
                    event.rawX >= (confirmPasswordInput.right - drawableEnd.bounds.width() - confirmPasswordInput.paddingEnd)) {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                    confirmPasswordInput.transformationMethod =
                        if (isConfirmPasswordVisible) null else PasswordTransformationMethod.getInstance()
                    confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        resetPasswordButton.setOnClickListener {
            val newPassword     = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Run through all validations before actually updating anything
            when {
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
                }
                // Enforce a minimum password length for basic security
                newPassword.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
                }
                // Make sure both fields match before saving
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> updatePassword(newPassword)
            }
        }
    }

    // Sends the new password to the database and redirects to login on success
    private fun updatePassword(newPassword: String) {
        val success = db.updatePassword(userEmail, newPassword)
        if (success) {
            Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_SHORT).show()
            // Clear the back stack so the user can't navigate back to this screen
            val intent = Intent(this, logIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            // Database update failed for some reason — let the user try again
            Toast.makeText(this, "Failed to reset password. Try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
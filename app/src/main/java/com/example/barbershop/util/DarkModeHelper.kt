package com.example.barbershop.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object DarkModeHelper {

    private const val PREFS_NAME    = "AppSettings"
    private const val KEY_DARK_MODE = "dark_mode_"

    // Build a unique SharedPreferences key for each user based on their email
    private fun key(email: String) = "$KEY_DARK_MODE${email.trim().lowercase()}"

    // Pull the currently logged-in user's email from UserPrefs
    private fun currentEmail(context: Context): String =
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("USER_EMAIL", "") ?: ""

    // Apply the dark mode setting for whoever is currently logged in
    fun apply(context: Context) {
        val isDark = isDarkMode(context)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else        AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Returns whether dark mode is currently on for the logged-in user
    fun isDarkMode(context: Context): Boolean {
        val email = currentEmail(context)
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(email), false)
    }

    fun isDarkModeForUser(context: Context, email: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(email), false)
    }

    // Apply the correct theme for a specific user
    fun applyForUser(context: Context, email: String) {
        val isDark = isDarkModeForUser(context, email)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else        AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Flips the current user's dark mode setting and immediately applies the change
    fun toggle(context: Context) {
        val email    = currentEmail(context)
        val newValue = !isDarkMode(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(email), newValue)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newValue) AppCompatDelegate.MODE_NIGHT_YES
            else          AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val email = currentEmail(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(email), enabled)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES
            else         AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Force light mode without touching any saved preferences
    fun resetToLight() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
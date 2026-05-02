package com.example.barbershop.user

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.R
import com.example.barbershop.auth.logIn

// About Us page — reuses the main landing layout and just scrolls to the About section
class about_us : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scrollView      = findViewById<ScrollView>(R.id.mainScrollView)
        val sectionHome     = findViewById<FrameLayout>(R.id.sectionHome)
        val sectionAbout    = findViewById<LinearLayout>(R.id.sectionAbout)
        val sectionServices = findViewById<LinearLayout>(R.id.sectionServices)
        val sectionContact  = findViewById<LinearLayout>(R.id.sectionContact)

        // Auto-scroll to the About section once the layout is ready
        sectionAbout.post {
            scrollView.smoothScrollTo(0, sectionAbout.top)
        }

        // Top nav links — each one just scrolls to its matching section
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionHome.top)
        }
        findViewById<TextView>(R.id.navAbout).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionAbout.top)
        }
        findViewById<TextView>(R.id.navServices).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionServices.top)
        }
        findViewById<TextView>(R.id.navContact).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionContact.top)
        }
        // Login link opens the login screen
        findViewById<TextView>(R.id.navLogin).setOnClickListener {
            startActivity(Intent(this, logIn::class.java))
        }
    }
}
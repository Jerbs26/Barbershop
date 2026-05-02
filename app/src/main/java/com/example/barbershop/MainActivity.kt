package com.example.barbershop

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.auth.logIn
import com.example.barbershop.util.Database
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {

    private val PAGE_SIZE = 3
    private var currentPage = 0
    private var allRatings: List<Map<String, String>> = emptyList()
    private lateinit var container: LinearLayout
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var tvPageIndicator: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scrollView      = findViewById<ScrollView>(R.id.mainScrollView)
        val topNav          = findViewById<LinearLayout>(R.id.topNav)
        val sectionHome     = findViewById<FrameLayout>(R.id.sectionHome)
        val sectionAbout    = findViewById<LinearLayout>(R.id.sectionAbout)
        val sectionServices = findViewById<LinearLayout>(R.id.sectionServices)
        findViewById<LinearLayout>(R.id.sectionReviews)
        val sectionContact  = findViewById<LinearLayout>(R.id.sectionContact)

        topNav.post {
            val screenHeight = resources.displayMetrics.heightPixels
            val params = sectionHome.layoutParams
            params.height = screenHeight - topNav.height
            sectionHome.layoutParams = params
        }

        container        = findViewById(R.id.reviewsContainer)
        btnPrev          = findViewById(R.id.btnPrevReview)
        btnNext          = findViewById(R.id.btnNextReview)
        tvPageIndicator  = findViewById(R.id.tvPageIndicator)

        loadReviews()

        btnPrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                renderPage()
            }
        }
        btnNext.setOnClickListener {
            val totalPages = getTotalPages()
            if (currentPage < totalPages - 1) {
                currentPage++
                renderPage()
            }
        }

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
        findViewById<TextView>(R.id.navLogin).setOnClickListener {
            startActivity(Intent(this, logIn::class.java))
        }

        findViewById<LinearLayout>(R.id.cardHaircut).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionServices.top)
        }
        findViewById<LinearLayout>(R.id.cardShaving).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionServices.top)
        }
        findViewById<LinearLayout>(R.id.cardStyling).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionServices.top)
        }
        findViewById<LinearLayout>(R.id.cardColoring).setOnClickListener {
            scrollView.smoothScrollTo(0, sectionServices.top)
        }

        findViewById<LinearLayout>(R.id.contactFacebook).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.facebook.com/UrbanRazor")))
        }
        findViewById<LinearLayout>(R.id.contactPhone).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+639094077216")))
        }
        findViewById<LinearLayout>(R.id.contactLocation).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=7021+Balimbing+St,+Comembo,+Taguig+City")))
        }

        val mapWebView = findViewById<WebView>(R.id.mapWebView)
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.settings.domStorageEnabled = true
        mapWebView.settings.setSupportZoom(true)
        mapWebView.settings.builtInZoomControls = true
        mapWebView.settings.displayZoomControls = false
        mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (url.contains("maps.google.com/maps") && url.contains("output=embed")) {
                    return false
                }
                return try {
                    val geoUri = Uri.parse(
                        "geo:0,0?q=7021+Balimbing+St,+Comembo,+Taguig+City,+Metro+Manila,+Philippines"
                    )
                    val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (mapsIntent.resolveActivity(packageManager) != null) {
                        startActivity(mapsIntent)
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin:0; padding:0; box-sizing:border-box; }
                    html, body { width:100%; height:100%; background:#1a1a1a; }
                    iframe { width:100%; height:100%; border:0; display:block; }
                </style>
            </head>
            <body>
                <iframe
                    src="https://maps.google.com/maps?q=7021+Balimbing+St,+Comembo,+Taguig+City,+Metro+Manila,+Philippines&z=18&output=embed"
                    allowfullscreen
                    loading="lazy">
                </iframe>
            </body>
            </html>
        """.trimIndent()

        mapWebView.loadDataWithBaseURL(
            "https://maps.google.com/",
            mapHtml,
            "text/html",
            "UTF-8",
            null
        )

        @SuppressLint("ClickableViewAccessibility")
        mapWebView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> {
                    scrollView.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    scrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    private fun loadReviews() {
        val tvAvgScore     = findViewById<TextView>(R.id.tvAvgScore)
        val tvAvgStars     = findViewById<TextView>(R.id.tvAvgStars)
        val tvTotalReviews = findViewById<TextView>(R.id.tvTotalReviews)

        allRatings = try {
            Database(this).getAllRatings() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        if (allRatings.isEmpty()) {
            container.removeAllViews()
            container.addView(TextView(this).apply {
                text = "No reviews yet. Be the first to share your experience!"
                textSize = 13f
                setTextColor(Color.parseColor("#AAAAAA"))
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            })
            tvAvgScore.text     = "–"
            tvAvgStars.text     = "☆☆☆☆☆"
            tvTotalReviews.text = "No reviews yet"
            btnPrev.visibility       = android.view.View.GONE
            btnNext.visibility       = android.view.View.GONE
            tvPageIndicator.visibility = android.view.View.GONE
            return
        }

        val avg = allRatings.mapNotNull { it["score"]?.toIntOrNull() }.average()
        tvAvgScore.text     = String.format("%.1f", avg)
        tvAvgStars.text     = starsText(avg.toInt())
        tvTotalReviews.text = "${allRatings.size} review${if (allRatings.size != 1) "s" else ""}"

        currentPage = 0
        renderPage()
    }

    private fun getTotalPages(): Int =
        ceil(allRatings.size.toDouble() / PAGE_SIZE).toInt()

    private fun renderPage() {
        container.removeAllViews()

        val totalPages = getTotalPages()
        val start = currentPage * PAGE_SIZE
        val end   = minOf(start + PAGE_SIZE, allRatings.size)

        allRatings.subList(start, end).forEach { rating ->
            container.addView(createReviewCard(rating))
        }

        tvPageIndicator.text = "Page ${currentPage + 1} of $totalPages"

        btnPrev.isEnabled = currentPage > 0
        btnPrev.alpha = if (currentPage > 0) 1f else 0.3f
        btnNext.isEnabled = currentPage < totalPages - 1
        btnNext.alpha = if (currentPage < totalPages - 1) 1f else 0.3f
    }

    private fun createReviewCard(rating: Map<String, String>): CardView {
        val score = rating["score"]?.toIntOrNull() ?: 0
        val name = rating["customer"]?.takeIf { it.isNotBlank() }
            ?: rating["user"]?.substringBefore("@") ?: "Customer"
        val comment = rating["comment"] ?: ""
        val date = rating["date"] ?: ""

        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            radius = dpToPx(10).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(Color.parseColor("#2A2A2A"))
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
        }

        val initials = name.take(2).uppercase()
        topRow.addView(TextView(this).apply {
            text = initials
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#5B7FE8"))
            val size = dpToPx(36)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = dpToPx(10)
            }
        })

        val nameCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        nameCol.addView(TextView(this).apply {
            text = name
            textSize = 13f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })
        nameCol.addView(TextView(this).apply {
            text = date
            textSize = 10f
            setTextColor(Color.parseColor("#888888"))
        })
        topRow.addView(nameCol)

        topRow.addView(TextView(this).apply {
            text = starsText(score)
            textSize = 14f
            setTextColor(Color.parseColor("#FFEB3B"))
        })

        inner.addView(topRow)

        if (comment.isNotBlank()) {
            inner.addView(TextView(this).apply {
                text = "\"$comment\""
                textSize = 13f
                setTextColor(Color.parseColor("#CCCCCC"))
                setTypeface(null, Typeface.ITALIC)
                setLineSpacing(0f, 1.4f)
            })
        }

        card.addView(inner)
        return card
    }

    private fun starsText(score: Int): String {
        val filled = score.coerceIn(0, 5)
        return "★".repeat(filled) + "☆".repeat(5 - filled)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
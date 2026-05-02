package com.example.barbershop.user

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.util.Database
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.UserSidebarHelper

class Products : AppCompatActivity() {

    private lateinit var btnNewBooking: Button
    private lateinit var btnBarbers: Button
    private lateinit var btnServices: Button
    private lateinit var btnProducts: Button
    private lateinit var btnHistory: Button
    private lateinit var btnRatings: Button
    private lateinit var btnLogout: Button
    private lateinit var btnCart: Button
    private lateinit var productsContainer: LinearLayout
    private val hamburger = UserSidebarHelper()
    private var cartTotal = 0.0
    private val cartItems = mutableListOf<String>()
    private val cartItemPrices = mutableMapOf<String, Double>()
    private var loggedInEmail: String = ""
    private val qtyMap = mutableMapOf<String, Int>()
    private val isDark get() = DarkModeHelper.isDarkMode(this)
    private val cardBg get() = if (isDark) Color.parseColor("#2C2C2C") else Color.WHITE
    private val textPrimary get() = if (isDark) Color.WHITE else Color.parseColor("#2C2C2C")
    private val textSecond get() = if (isDark) Color.parseColor("#AAAAAA") else Color.parseColor("#666666")
    private val qtyTextColor get() = if (isDark) Color.WHITE else Color.parseColor("#2C2C2C")

    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        loggedInEmail = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("USER_EMAIL", "") ?: ""

        hamburger.setup(
            context     = this,
            sidebar     = findViewById(R.id.sidebar),
            contentArea = findViewById(R.id.contentArea)
        )

        btnNewBooking     = findViewById(R.id.btnNewBooking)
        btnBarbers        = findViewById(R.id.btnBarbers)
        btnServices       = findViewById(R.id.btnServices)
        btnProducts       = findViewById(R.id.btnProducts)
        btnHistory        = findViewById(R.id.btnHistory)
        btnRatings        = findViewById(R.id.btnRatings)
        btnLogout         = findViewById(R.id.btnLogout)
        btnCart           = findViewById(R.id.btnCart)
        productsContainer = findViewById(R.id.productsContainer)

        restoreCartState()
        loadUserSidebar()
        setupNavigation()
        loadProducts()

        btnCart.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                hamburger.closeSidebar()
                startActivity(Intent(this, Cart::class.java).apply {
                    putExtra("email", loggedInEmail)
                })
            }
        }
    }


    override fun onResume() {
        super.onResume()
        restoreCartState()
    }

    private fun restoreCartState() {
        val prefs      = getSharedPreferences("CartPrefs", MODE_PRIVATE)
        val savedItems = prefs.getString("cart_items", "") ?: ""

        cartItems.clear()
        cartItemPrices.clear()

        if (savedItems.isNotEmpty()) {
            cartItems.addAll(savedItems.split("|").filter { it.isNotBlank() })
        }

        cartItems.forEach { key ->
            cartItemPrices[key] = prefs.getFloat("price_$key", 0f).toDouble()
        }

        cartTotal = cartItems.sumOf { key ->
            val qty       = key.substringAfterLast(" x").trim().toIntOrNull() ?: 1
            val unitPrice = cartItemPrices[key] ?: 0.0
            qty * unitPrice
        }

        updateCartButton()
    }

    private fun saveCartState() {
        getSharedPreferences("CartPrefs", MODE_PRIVATE).edit().apply {
            putString("cart_items", cartItems.joinToString("|"))
            putFloat("cart_total",  cartTotal.toFloat())
            putInt("cart_count",    cartItems.size)
            cartItemPrices.forEach { (key, price) ->
                putFloat("price_$key", price.toFloat())
            }
            apply()
        }
    }

    private fun updateCartButton() {
        btnCart.text = if (cartItems.isEmpty()) "🛒 Cart" else "🛒 Cart (${cartItems.size})"
    }

    private fun loadProducts() {
        productsContainer.removeAllViews()
        val db = Database(this)
        val products = db.getAllProducts()

        if (products.isEmpty()) {
            productsContainer.addView(TextView(this).apply {
                text     = "No products available."
                textSize = 14f
                setTextColor(textSecond)
                gravity  = Gravity.CENTER
                setPadding(0, 48, 0, 0)
            })
            return
        }

        for (product in products) {
            val id    = product["id"] ?: continue
            val name  = product["name"]?.takeIf { it.isNotBlank() } ?: continue
            val desc  = product["desc"] ?: ""
            val price = product["price"] ?: "₱0.00"
            val stock = (product["stock"] ?: "0").toIntOrNull() ?: 0

            val priceValue = price.replace("₱", "").replace(",", "").trim()
                .toDoubleOrNull() ?: 0.0

            qtyMap[id] = 1

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(cardBg)
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                elevation = dpToPx(2).toFloat()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, dpToPx(12)) }
            }

            when {
                stock == 0 -> card.addView(stockBadge("OUT OF STOCK", "#FF4444", Color.WHITE))
                stock in 1..7 -> card.addView(stockBadge("LOW STOCK",    "#FFD700", Color.BLACK))
            }

            card.addView(TextView(this).apply {
                text = name
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(textPrimary)
            })

            if (desc.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(textSecond)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, dpToPx(8), 0, 0) }
                })
            }

            card.addView(TextView(this).apply {
                text = if (stock == 0) "Out of stock" else "Stock: $stock available"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (stock in 1..7) Color.parseColor("#FF6B6B") else textSecond)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(8), 0, 0) }
            })

            card.addView(TextView(this).apply {
                text = price
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#4A90E2"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(8), 0, 0) }
            })

            val qtyRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(12), 0, 0) }
            }

            val btnSize = dpToPx(36)

            val btnDecrease = Button(this).apply {
                text = "-"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#4A90E2"))
                setPadding(0, 0, 0, 0)
                isEnabled    = stock > 0
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            }

            val tvQty = TextView(this).apply {
                text = "1"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(qtyTextColor)
                layoutParams = LinearLayout.LayoutParams(dpToPx(45), LinearLayout.LayoutParams.WRAP_CONTENT)
                    .also { it.setMargins(dpToPx(4), 0, dpToPx(4), 0) }
            }

            val btnIncrease = Button(this).apply {
                text = "+"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#4A90E2"))
                setPadding(0, 0, 0, 0)
                isEnabled    = stock > 0
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            }

            val btnAddToCart = TextView(this).apply {
                text = "Add to Cart"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    if (stock > 0) Color.parseColor("#4A90E2")
                    else Color.parseColor("#555555")
                )
                gravity = Gravity.CENTER
                isSingleLine = true
                isClickable = stock > 0
                layoutParams = LinearLayout.LayoutParams(0, btnSize, 1f)
                    .also { it.setMargins(dpToPx(8), 0, 0, 0) }
            }

            btnDecrease.setOnClickListener {
                val cur = qtyMap[id] ?: 1
                if (cur > 1) {
                    qtyMap[id] = cur - 1
                    tvQty.text = qtyMap[id].toString()
                }
            }

            btnIncrease.setOnClickListener {
                val cur = qtyMap[id] ?: 1
                if (cur < stock) {
                    qtyMap[id] = cur + 1
                    tvQty.text = qtyMap[id].toString()
                } else {
                    Toast.makeText(this, "Max stock reached", Toast.LENGTH_SHORT).show()
                }
            }

            btnAddToCart.setOnClickListener {
                if (stock == 0) return@setOnClickListener

                val qtyToAdd = qtyMap[id] ?: 1

                val existingKey = cartItems.firstOrNull { key ->
                    key.substringBeforeLast(" x").trim() == name
                }

                if (existingKey != null) {
                    val existingQty = existingKey.substringAfterLast(" x").trim().toIntOrNull() ?: 1
                    val newQty      = existingQty + qtyToAdd

                    getSharedPreferences("CartPrefs", MODE_PRIVATE)
                        .edit().remove("price_$existingKey").apply()

                    val newKey = "$name x$newQty"
                    val idx    = cartItems.indexOf(existingKey)
                    cartItems[idx]          = newKey
                    cartItemPrices.remove(existingKey)
                    cartItemPrices[newKey]  = priceValue

                    Toast.makeText(this, "Updated $name → Qty $newQty", Toast.LENGTH_SHORT).show()
                } else {
                    val newKey = "$name x$qtyToAdd"
                    cartItems.add(newKey)
                    cartItemPrices[newKey] = priceValue
                    Toast.makeText(this, "Added $qtyToAdd × $name to cart", Toast.LENGTH_SHORT).show()
                }

                cartTotal = cartItems.sumOf { key ->
                    val qty       = key.substringAfterLast(" x").trim().toIntOrNull() ?: 1
                    val unitPrice = cartItemPrices[key] ?: 0.0
                    qty * unitPrice
                }

                updateCartButton()
                saveCartState()
            }

            qtyRow.addView(btnDecrease)
            qtyRow.addView(tvQty)
            qtyRow.addView(btnIncrease)
            qtyRow.addView(btnAddToCart)
            card.addView(qtyRow)
            productsContainer.addView(card)
        }
    }

    private fun stockBadge(label: String, bgColor: String, textColor: Int): TextView =
        TextView(this).apply {
            text     = label
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textColor)
            setBackgroundColor(Color.parseColor(bgColor))
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
        }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun loadUserSidebar() {
        val prefs    = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email    = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = email.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase()
        else namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        findViewById<TextView>(R.id.userEmail).text     = email
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        dialog.show()
    }

    private fun setupNavigation() {
        btnNewBooking.setOnClickListener { hamburger.closeSidebar(); startActivity(Intent(this, Dashboard::class.java)) }
        btnBarbers.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Barbers::class.java)) }
        btnServices.setOnClickListener   { hamburger.closeSidebar(); startActivity(Intent(this, Services::class.java)) }
        btnProducts.setOnClickListener   { hamburger.closeSidebar() }
        btnHistory.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, History::class.java)) }
        btnRatings.setOnClickListener    { hamburger.closeSidebar(); startActivity(Intent(this, Ratings::class.java)) }
        btnLogout.setOnClickListener     { showLogoutDialog() }
    }
}
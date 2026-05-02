package com.example.barbershop.user

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.barbershop.R
import java.io.ByteArrayOutputStream

class Cart : AppCompatActivity() {

    private var cartTotal = 0.0
    private var loggedInEmail = ""
    private val cartItems = mutableListOf<String>()
    private val cartItemPrices = mutableMapOf<String, Double>()
    private var gcashDialog: Dialog? = null
    private var selectedScreenshotUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedScreenshotUri = uri
                gcashDialog?.findViewById<ImageView>(R.id.ivScreenshotPreview)?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        loggedInEmail = intent.getStringExtra("email")
            ?: getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("USER_EMAIL", "") ?: ""

        loadCartFromPrefs()
        renderCartItems()

        findViewById<Button>(R.id.btnContinueShopping).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnProceedCheckout).setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isFinishing || isDestroyed) return@setOnClickListener

            val amountString = "₱${String.format("%.2f", cartTotal)}"
            showGcashDialog(amountString) {

                if (isFinishing || isDestroyed) return@showGcashDialog

                val base64Screenshot = uriToBase64(selectedScreenshotUri)
                val itemsSnapshot    = cartItems.joinToString(", ")
                clearCartPrefs()
                startActivity(Intent(this, OrderConfirmation::class.java).apply {
                    putExtra("email",          loggedInEmail)
                    putExtra("items",          itemsSnapshot)
                    putExtra("totalAmount",    amountString)
                    putExtra("screenshotPath", base64Screenshot)
                })
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gcashDialog?.dismiss()
        gcashDialog = null
    }

    private fun loadCartFromPrefs() {
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

        recalculateTotal()
    }

    private fun recalculateTotal() {
        cartTotal = cartItems.sumOf { key ->
            val qty       = key.substringAfterLast(" x").trim().toIntOrNull() ?: 1
            val unitPrice = cartItemPrices[key] ?: 0.0
            qty * unitPrice
        }
    }

    private fun saveCartToPrefs() {
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

    private fun clearCartPrefs() {
        getSharedPreferences("CartPrefs", MODE_PRIVATE).edit().clear().apply()
    }

    private fun removeItem(itemKey: String) {
        cartItems.remove(itemKey)
        cartItemPrices.remove(itemKey)

        getSharedPreferences("CartPrefs", MODE_PRIVATE)
            .edit().remove("price_$itemKey").apply()

        recalculateTotal()
        saveCartToPrefs()
        renderCartItems()

        val name = itemKey.substringBeforeLast(" x").trim()
        Toast.makeText(this, "\"$name\" removed from cart", Toast.LENGTH_SHORT).show()
    }

    private fun renderCartItems() {
        val container = findViewById<LinearLayout>(R.id.cartItemsContainer)

        val toRemove = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.tag == "cart_item_card") toRemove.add(child)
        }
        toRemove.forEach { container.removeView(it) }

        if (cartItems.isEmpty()) {
            val emptyText = TextView(this).apply {
                tag      = "cart_item_card"
                text     = "Your cart is empty."
                textSize = 14f
                setTextColor(Color.parseColor("#888888"))
                gravity  = Gravity.CENTER
                setPadding(0, 48, 0, 48)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(emptyText, 0)
        } else {
            // reversed() so newest-added item appears at the top
            cartItems.reversed().forEachIndexed { index, itemKey ->
                val name = itemKey.substringBeforeLast(" x").trim()
                val qty  = itemKey.substringAfterLast(" x").trim().toIntOrNull() ?: 1
                val card = buildItemCard(itemKey, name, qty)
                card.tag = "cart_item_card"
                container.addView(card, index)
            }
        }

        updateSummaryUI()
    }

    private fun updateSummaryUI() {
        findViewById<TextView>(R.id.tvSummaryItemLabel).text =
            "Items (${cartItems.size}):"
        findViewById<TextView>(R.id.tvSummaryItemTotal).text =
            "₱${String.format("%.2f", cartTotal)}"
        findViewById<TextView>(R.id.tvSummaryTotal).text =
            "₱${String.format("%.2f", cartTotal)}"
    }

    private fun buildItemCard(itemKey: String, name: String, qty: Int): CardView {
        val unitPrice = cartItemPrices[itemKey] ?: 0.0
        val lineTotal = qty * unitPrice

        val card = CardView(this).apply {
            radius        = 24f
            cardElevation = 8f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 24 }
        }

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }

        val info = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        info.addView(TextView(this).apply {
            text     = name
            textSize = 14f
            setTextColor(Color.parseColor("#2C2C2C"))
            setTypeface(null, Typeface.BOLD)
        })
        info.addView(TextView(this).apply {
            text     = "Qty: $qty"
            textSize = 11f
            setTextColor(Color.parseColor("#888888"))
        })
        info.addView(TextView(this).apply {
            text     = "₱${String.format("%.2f", unitPrice)} each"
            textSize = 11f
            setTextColor(Color.parseColor("#888888"))
        })

        val tvLineTotal = TextView(this).apply {
            text     = "₱${String.format("%.2f", lineTotal)}"
            textSize = 13f
            setTextColor(Color.parseColor("#4A90E2"))
            setTypeface(null, Typeface.BOLD)
            gravity  = Gravity.END
        }

        row.addView(info)
        row.addView(tvLineTotal)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.topMargin = 12; it.bottomMargin = 8 }
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        val btnRemove = Button(this).apply {
            text = "✕  Remove"
            textSize = 11f
            isAllCaps = false
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
            setTextColor(Color.parseColor("#E53935"))
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 8, 24, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.END }

            setOnClickListener {
                if (isFinishing || isDestroyed) return@setOnClickListener

                AlertDialog.Builder(this@Cart)
                    .setTitle("Remove Item")
                    .setMessage("Remove \"$name\" from your cart?")
                    .setPositiveButton("Remove") { _, _ -> removeItem(itemKey) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        outer.addView(row)
        outer.addView(divider)
        outer.addView(btnRemove)
        card.addView(outer)
        return card
    }

    private fun uriToBase64(uri: Uri?): String {
        if (uri == null) return ""
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return ""
            val bitmap      = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return ""
            val resized = resizeBitmap(bitmap, 800)
            val out     = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio     = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    private fun showGcashDialog(amount: String, onConfirmed: () -> Unit) {
        selectedScreenshotUri = null

        val dialog = Dialog(this)
        gcashDialog = dialog
        dialog.setContentView(R.layout.activity_gcash_payment_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        dialog.findViewById<TextView>(R.id.tvAmountToPay).text = amount

        dialog.findViewById<Button>(R.id.btnChooseScreenshot).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        dialog.findViewById<Button>(R.id.btnCancelPayment).setOnClickListener {
            dialog.dismiss()
            gcashDialog = null
        }
        dialog.findViewById<Button>(R.id.btnConfirmPayment).setOnClickListener {
            if (selectedScreenshotUri == null) {
                Toast.makeText(
                    this,
                    "Please upload your GCash payment screenshot",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            gcashDialog = null
            onConfirmed()
        }

        dialog.show()
    }
}
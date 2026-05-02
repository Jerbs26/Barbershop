package com.example.barbershop.admin

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import com.example.barbershop.util.AppData
import com.example.barbershop.admin.BaseAdminActivity
import com.example.barbershop.R

class AdminProducts : BaseAdminActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_products)
        setupBase(R.id.btnAdminProducts)
        loadContent()
    }

    private fun loadContent() {
        val headerRow = makeHorizontalRow(dpToPx(16))
        val title = makePageTitle("Manage Products")
        title.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        headerRow.addView(title)
        val addBtn = makeActionButton("+ Add Product", "#4A90E2")
        addBtn.setOnClickListener { showProductDialog(null) }
        headerRow.addView(addBtn)
        contentArea.addView(headerRow)

        reloadAndRender()
    }

    private fun reloadAndRender() {
        Thread {
            val freshList = mutableListOf<MutableMap<String, String>>()
            try {
                db.getAllProducts().forEach { row ->
                    freshList.add(mutableMapOf(
                        "id"    to (row["id"]    ?: ""),
                        "name"  to (row["name"]  ?: ""),
                        "desc"  to (row["desc"]  ?: ""),
                        "price" to (row["price"] ?: ""),
                        "stock" to (row["stock"] ?: "0")
                    ))
                }
                synchronized(AppData.productsList) {
                    AppData.productsList.clear()
                    AppData.productsList.addAll(freshList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread { renderProductsCards(freshList) }
        }.start()
    }

    private fun renderProductsCards(products: List<MutableMap<String, String>>) {
        val keepCount = if (isSmallScreen && contentArea.childCount > 0
            && contentArea.getChildAt(0) is Button
        ) 2 else 1
        while (contentArea.childCount > keepCount) {
            contentArea.removeViewAt(contentArea.childCount - 1)
        }

        if (isSmallScreen) {
            products.forEach { p ->
                val stockVal = p["stock"]?.toIntOrNull() ?: 0
                val (stockColor, stockLabel) = when {
                    stockVal == 0  -> "#E74C3C" to "Out of Stock"
                    stockVal <= 7  -> "#F5A623" to "Low Stock ($stockVal)"
                    else           -> "#4CAF50" to "In Stock ($stockVal)"
                }
                val card  = makeCard()
                val inner = verticalLayout(0)
                inner.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))

                val topRow = makeHorizontalRow(dpToPx(4))
                val nameTv = makeText(p["name"]!!, 15f, "#1A1A1A", true)
                nameTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                topRow.addView(nameTv)
                topRow.addView(makeText(p["price"]!!, 15f, "#333333", true))
                inner.addView(topRow)

                val descLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                descLp.setMargins(0, 0, 0, dpToPx(6))
                val descTv = makeText(p["desc"]!!, 12f, "#666666")
                descTv.layoutParams = descLp
                inner.addView(descTv)

                val badgeLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                badgeLp.setMargins(0, 0, 0, dpToPx(10))
                val badge = makeStatusBadge(stockLabel)
                badge.background = roundedBg(stockColor)
                badge.layoutParams = badgeLp
                inner.addView(badge)

                val btnRow = makeHorizontalRow(0)
                val editBtn = makeActionButton("Edit", "#F5A623")
                val editLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(30))
                editLp.setMargins(0, 0, dpToPx(8), 0)
                editBtn.layoutParams = editLp
                editBtn.setOnClickListener { showProductDialog(p) }
                btnRow.addView(editBtn)

                val deleteBtn = makeActionButton("Delete", "#E74C3C")
                deleteBtn.setOnClickListener {
                    showDeleteConfirm("product", p["name"]!!) {
                        Thread {
                            try { db.deleteProduct(p["id"]!!) } catch (e: Exception) { e.printStackTrace() }
                            runOnUiThread {
                                showToast("Product deleted.")
                                reloadAndRender()
                            }
                        }.start()
                    }
                }
                btnRow.addView(deleteBtn)
                inner.addView(btnRow)
                card.addView(inner)
                contentArea.addView(card)
            }
            if (products.isEmpty()) {
                val empty = makeText("No products found.", 13f, "#888888")
                empty.setPadding(0, dpToPx(16), 0, 0)
                contentArea.addView(empty)
            }
        } else {
            val tableCard  = makeCard()
            val tableInner = verticalLayout(dpToPx(12))

            val hdr = makeHorizontalRow(0).apply {
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            }
            listOf("Product Name" to 1f, "Description" to 2f, "Price" to 0.6f, "Stock" to 0.8f, "Actions" to 0.8f)
                .forEach { (h, w) ->
                    val tv = makeText(h, 11f, "#666666", true)
                    tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
                    hdr.addView(tv)
                }
            tableInner.addView(hdr)

            products.forEach { p ->
                val stockVal = p["stock"]?.toIntOrNull() ?: 0
                val (stockColor, stockLabel) = when {
                    stockVal == 0  -> "#E74C3C" to "$stockVal (Out of Stock)"
                    stockVal <= 7  -> "#F5A623" to "$stockVal (Low Stock)"
                    else           -> "#4CAF50" to "$stockVal"
                }
                val row = makeHorizontalRow(0).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(10), dpToPx(12), dpToPx(10), dpToPx(12))
                    setBackgroundColor(Color.WHITE)
                }

                val nameTv = makeText(p["name"]!!, 12f, "#1A1A1A", true)
                nameTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(nameTv)

                val descTv = makeText(p["desc"]!!, 11f, "#666666")
                descTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                row.addView(descTv)

                val priceTv = makeText(p["price"]!!, 12f, "#333333")
                priceTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                row.addView(priceTv)

                val stockTv = makeText(stockLabel, 11f, stockColor, true)
                stockTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                row.addView(stockTv)

                val actionCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                }

                val editBtn = makeActionButton("Edit", "#F5A623")
                val editLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                editLp.setMargins(0, 0, 0, dpToPx(4))
                editBtn.layoutParams = editLp
                editBtn.setOnClickListener { showProductDialog(p) }
                actionCol.addView(editBtn)

                val deleteBtn = makeActionButton("Delete", "#E74C3C")
                deleteBtn.setOnClickListener {
                    showDeleteConfirm("product", p["name"]!!) {
                        Thread {
                            try { db.deleteProduct(p["id"]!!) } catch (e: Exception) { e.printStackTrace() }
                            runOnUiThread {
                                showToast("Product deleted.")
                                reloadAndRender()
                            }
                        }.start()
                    }
                }
                actionCol.addView(deleteBtn)
                row.addView(actionCol)
                tableInner.addView(row)
                tableInner.addView(makeDivider())
            }

            if (products.isEmpty()) {
                val empty = makeText("No products found.", 13f, "#888888")
                empty.setPadding(0, dpToPx(16), 0, 0)
                tableInner.addView(empty)
            }
            tableCard.addView(tableInner)
            contentArea.addView(tableCard)
        }
    }

    private fun showProductDialog(existing: MutableMap<String, String>?) {
        val isEdit = existing != null
        val layout = verticalLayout(dpToPx(16))
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(4))

        val titleLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        titleLp.setMargins(0, 0, 0, dpToPx(16))
        val titleView = makeText(if (isEdit) "Edit Product" else "Add Product", 17f, "#1A1A1A", true)
        titleView.layoutParams = titleLp
        layout.addView(titleView)

        val nameEt  = makeDialogEditText("Product Name",   existing?.get("name")  ?: "")
        val descEt  = makeDialogEditText("Description",    existing?.get("desc")  ?: "")
        val priceEt = makeDialogEditText("Price (₱)",      existing?.get("price")?.replace("₱", "") ?: "")
        val stockEt = makeDialogEditText("Stock Quantity", existing?.get("stock") ?: "0")
        (stockEt.tag as EditText).inputType = InputType.TYPE_CLASS_NUMBER
        (priceEt.tag as EditText).inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        listOf(nameEt, descEt, priceEt, stockEt).forEach { layout.addView(it) }

        AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val nameVal  = (nameEt.tag  as EditText).text.toString()
                val descVal  = (descEt.tag  as EditText).text.toString()
                val priceVal = "₱${(priceEt.tag as EditText).text}"
                val stockVal = (stockEt.tag as EditText).text.toString().toIntOrNull() ?: 0

                if (nameVal.isBlank()) { showToast("Name is required!"); return@setPositiveButton }

                Thread {
                    try {
                        if (isEdit) {
                            db.updateProduct(existing!!["id"]!!, nameVal, descVal, priceVal, stockVal)
                        } else {
                            db.insertProduct(nameVal, descVal, priceVal, stockVal)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    runOnUiThread {
                        showToast(if (isEdit) "Product updated!" else "Product added!")
                        reloadAndRender()
                    }
                }.start()
            }.setNegativeButton("Cancel", null).show()
    }
}
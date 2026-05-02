package com.example.barbershop.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class UserSidebarHelper {

    private var isSidebarOpen = false
    private var sidebar: LinearLayout? = null

    fun setup(context: Context, sidebar: LinearLayout, contentArea: LinearLayout) {
        this.sidebar = sidebar

        val screenWidthDp = (context.resources.displayMetrics.widthPixels /
                context.resources.displayMetrics.density).toInt()

        if (screenWidthDp < 480) {
            sidebar.visibility = View.GONE
            isSidebarOpen = false

            val btn = buildHamburgerButton(context)
            val stickyHeader = findStickyHeader(contentArea)

            if (stickyHeader != null) {
                stickyHeader.addView(btn, 0)
            } else {
                contentArea.setPadding(
                    contentArea.paddingLeft,
                    0,
                    contentArea.paddingRight,
                    contentArea.paddingBottom
                )
                contentArea.addView(btn, 0)
            }
        }
    }

    fun highlightActiveButton(allNavButtons: List<Button>, activeButton: Button) {
        allNavButtons.forEach { btn ->
            btn.setTextColor(Color.WHITE)
            btn.backgroundTintList = null
            btn.background = ColorDrawable(Color.TRANSPARENT)
        }
        activeButton.setTextColor(Color.parseColor("#FFEB3B"))
        activeButton.background = ColorDrawable(Color.TRANSPARENT)
    }

    private fun findStickyHeader(contentArea: LinearLayout): LinearLayout? {
        for (i in 0 until contentArea.childCount) {
            val child = contentArea.getChildAt(i)
            if (child is LinearLayout && child.tag == "stickyHeader") return child
        }
        return null
    }

    private fun buildHamburgerButton(context: Context): Button = Button(context).apply {
        text = "☰"
        textSize = 22f
        setTextColor(Color.parseColor("#F5A623"))
        background = ColorDrawable(Color.TRANSPARENT)
        setPadding(0, 0, 0, 0)
        minWidth = 0
        minHeight = 0
        stateListAnimator = null
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(context, 44), dpToPx(context, 44)
        ).also { lp -> lp.setMargins(0, 0, dpToPx(context, 8), 0) }
        setOnClickListener { toggle() }
    }

    private fun toggle() {
        val sb = sidebar ?: return
        if (isSidebarOpen) {
            sb.visibility = View.GONE
            isSidebarOpen = false
        } else {
            sb.visibility = View.VISIBLE
            isSidebarOpen = true
        }
    }

    fun closeSidebar() {
        val sb = sidebar ?: return
        if (isSidebarOpen) {
            sb.visibility = View.GONE
            isSidebarOpen = false
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
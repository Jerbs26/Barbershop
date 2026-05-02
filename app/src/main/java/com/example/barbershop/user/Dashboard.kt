package com.example.barbershop.user

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershop.MainActivity
import com.example.barbershop.R
import com.example.barbershop.util.DarkModeHelper
import com.example.barbershop.util.Database
import com.example.barbershop.util.UserSidebarHelper
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@androidx.camera.core.ExperimentalGetImage
class Dashboard : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var spinnerBarber: Spinner
    private lateinit var spinnerService: Spinner
    private lateinit var etDate: EditText
    private lateinit var tvTimeSlotNote: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnBookNow: Button
    private lateinit var btnLogout: Button
    private lateinit var btnBarbers: Button
    private lateinit var btnServices: Button
    private lateinit var btnProducts: Button
    private lateinit var btnHistory: Button
    private lateinit var btnRatings: Button
    private lateinit var btnHaircutFinder: Button
    private lateinit var btnDarkMode: Button

    private val hamburger = UserSidebarHelper()
    private val calendar = Calendar.getInstance()
    private var selectedTimeSlot: String? = null
    private var selectedServicePrice: Double = 0.0
    private var loggedInEmail: String = ""

    private var gcashDialog: Dialog? = null
    private var selectedScreenshotUri: Uri? = null

    // Real-time polling
    private val pollHandler = Handler(Looper.getMainLooper())
    private var lastBarberSnapshot  = listOf<Map<String, String>>()
    private var lastServiceSnapshot = listOf<Map<String, String>>()
    private var currentServices = listOf<Map<String, String>>()

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForDataChanges()
            pollHandler.postDelayed(this, 3_000)
        }
    }

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

    // Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        DarkModeHelper.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val emailFromIntent = intent.getStringExtra("email") ?: ""
        if (emailFromIntent.isNotEmpty()) {
            prefs.edit().putString("USER_EMAIL", emailFromIntent).apply()
            loggedInEmail = emailFromIntent
        } else {
            loggedInEmail = prefs.getString("USER_EMAIL", "") ?: ""
        }

        hamburger.setup(
            context     = this,
            sidebar     = findViewById(R.id.sidebar),
            contentArea = findViewById(R.id.contentArea)
        )

        loadUserSidebar()

        etUsername       = findViewById(R.id.etUsername)
        spinnerBarber    = findViewById(R.id.spinnerBarber)
        spinnerService   = findViewById(R.id.spinnerService)
        etDate           = findViewById(R.id.etDate)
        tvTimeSlotNote   = findViewById(R.id.tvTimeSlotNote)
        tvTotalAmount    = findViewById(R.id.tvTotalAmount)
        btnBookNow       = findViewById(R.id.btnBookNow)
        btnLogout        = findViewById(R.id.btnLogout)
        btnBarbers       = findViewById(R.id.btnBarbers)
        btnServices      = findViewById(R.id.btnServices)
        btnProducts      = findViewById(R.id.btnProducts)
        btnHistory       = findViewById(R.id.btnHistory)
        btnRatings       = findViewById(R.id.btnRatings)
        btnHaircutFinder = findViewById(R.id.btnHaircutFinder)
        btnDarkMode      = findViewById(R.id.btnDarkMode)

        setupBarberSpinner()
        setupServiceSpinner()
        setupDatePicker()
        setupFormValidation()
        setupBookNow()
        setupLogout()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Start polling every 3 seconds while the screen is visible
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        // Stop polling when the screen goes to background — prevents memory leaks
        pollHandler.removeCallbacks(pollRunnable)
    }

    // Real-time polling
    private fun checkForDataChanges() {
        Thread {
            val db = Database(this)
            val freshBarbers = try {
                db.getAllBarbers().filter { it["status"] == "Active" }
            } catch (e: Exception) {
                null // null = DB error, skip update
            }
            val freshServices = try {
                db.getAllServices()
            } catch (e: Exception) {
                null
            }
            db.close()

            val barbersChanged  = freshBarbers  != null && freshBarbers  != lastBarberSnapshot
            val servicesChanged = freshServices != null && freshServices != lastServiceSnapshot

            if (barbersChanged || servicesChanged) {
                runOnUiThread {
                    if (barbersChanged && freshBarbers != null) {
                        lastBarberSnapshot = freshBarbers
                        refreshBarberSpinner(freshBarbers)
                    }
                    if (servicesChanged && freshServices != null) {
                        lastServiceSnapshot = freshServices
                        refreshServiceSpinner(freshServices)
                    }
                }
            }
        }.start()
    }

    // Barber spinner
    private fun setupBarberSpinner() {
        val db = Database(this)
        val activeBarbers = try {
            db.getAllBarbers().filter { it["status"] == "Active" }
        } catch (e: Exception) {
            emptyList()
        } finally {
            db.close()
        }
        lastBarberSnapshot = activeBarbers
        refreshBarberSpinner(activeBarbers)
    }

    private fun refreshBarberSpinner(activeBarbers: List<Map<String, String>>) {
        // Remember whatever the user had selected so we can restore it
        val previouslySelected = spinnerBarber.selectedItem?.toString() ?: ""

        val barberNames = mutableListOf("Choose a barber...")
        activeBarbers.forEach { barberNames.add(it["name"] ?: "") }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, barberNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBarber.adapter = adapter

        // Restore previous selection if the barber still exists
        val restoredIndex = barberNames.indexOf(previouslySelected)
        if (restoredIndex > 0) {
            spinnerBarber.setSelection(restoredIndex)
        }

        spinnerBarber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                // Only clear date/slots when the selection actually changes
                if (position == 0) {
                    etDate.setText("")
                    selectedTimeSlot = null
                    hideTimeSlots()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Service spinner
    private fun setupServiceSpinner() {
        val db = Database(this)
        val services = try {
            db.getAllServices()
        } catch (e: Exception) {
            emptyList()
        } finally {
            db.close()
        }
        lastServiceSnapshot = services
        currentServices = services
        refreshServiceSpinner(services)
    }

    private fun refreshServiceSpinner(services: List<Map<String, String>>) {
        val previouslySelected = spinnerService.selectedItem?.toString() ?: ""

        // Keep a reference so the listener can read prices without another DB call
        currentServices = services

        val serviceLabels = mutableListOf("Choose a service...")
        services.forEach { svc ->
            serviceLabels.add("${svc["name"] ?: ""} - ${svc["price"] ?: ""}")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerService.adapter = adapter

        // Restore previous selection if the service still exists
        val restoredIndex = serviceLabels.indexOf(previouslySelected)
        if (restoredIndex > 0) {
            spinnerService.setSelection(restoredIndex)
        }

        spinnerService.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedServicePrice = if (position > 0 && position <= currentServices.size) {
                    val rawPrice = currentServices[position - 1]["price"] ?: "0"
                    // Strip peso sign and commas before parsing (e.g. "₱180.00" → 180.0)
                    rawPrice.replace("₱", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                } else {
                    0.0
                }
                updateTotalAmount()
                checkAndShowTimeSlotsWithRestDayCheck()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Theme helper
    private fun themeColor(attr: Int): Int {
        val ta = theme.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, Color.BLACK)
        ta.recycle()
        return color
    }

    // Sidebar
    private fun loadUserSidebar() {
        val prefs    = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val email    = prefs.getString("USER_EMAIL", "user@gmail.com") ?: "user@gmail.com"
        val namePart = email.substringBefore("@")
        val initials = if (namePart.length >= 2) namePart.take(2).uppercase()
        else namePart.uppercase()
        findViewById<TextView>(R.id.profileCircle).text = initials
        findViewById<TextView>(R.id.userEmail).text     = email
    }

    // Screenshot / GCash dialog
    private fun uriToBase64(uri: Uri?): String {
        if (uri == null) return ""
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return ""
            val bitmap      = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return ""
            val resized      = resizeBitmap(bitmap, 800)
            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
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

    // Book Now
    private fun setupBookNow() {
        btnBookNow.setOnClickListener {
            val username   = etUsername.text.toString().trim()
            val barberPos  = spinnerBarber.selectedItemPosition
            val servicePos = spinnerService.selectedItemPosition
            val date       = etDate.text.toString().trim()

            when {
                username.isEmpty() -> {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                barberPos == 0 -> {
                    Toast.makeText(this, "Please select a barber", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                servicePos == 0 -> {
                    Toast.makeText(this, "Please select a service", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                date.isEmpty() -> {
                    Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectedTimeSlot == null -> {
                    Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val amountString = "₱${String.format("%.2f", selectedServicePrice)}"
            showGcashDialog(amountString) {
                val base64Screenshot = uriToBase64(selectedScreenshotUri)
                startActivity(Intent(this, confirmation::class.java).apply {
                    putExtra("username",       etUsername.text.toString().trim())
                    putExtra("barber",         spinnerBarber.selectedItem.toString())
                    putExtra("service",        spinnerService.selectedItem.toString())
                    putExtra("date",           etDate.text.toString().trim())
                    putExtra("timeSlot",       selectedTimeSlot)
                    putExtra("totalAmount",    amountString)
                    putExtra("email",          loggedInEmail)
                    putExtra("screenshotPath", base64Screenshot)
                })
            }
        }
    }

    // Navigation
    private fun setupNavigation() {
        btnBarbers.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, Barbers::class.java))
        }
        btnServices.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, Services::class.java))
        }
        btnProducts.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, Products::class.java))
        }
        btnHistory.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, History::class.java))
        }
        btnRatings.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, Ratings::class.java))
        }
        btnHaircutFinder.setOnClickListener {
            hamburger.closeSidebar()
            startActivity(Intent(this, FaceShapeActivity::class.java))
        }
        updateDarkModeButton()
        btnDarkMode.setOnClickListener {
            hamburger.closeSidebar()
            DarkModeHelper.toggle(this)
            recreate()
        }
    }

    private fun setupLogout() {
        btnLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
            DarkModeHelper.resetToLight()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        dialog.show()
    }

    // Date picker
    private fun setupDatePicker() {
        etDate.setOnClickListener {
            if (spinnerBarber.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select a barber first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now   = Calendar.getInstance()
            val year  = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH)
            val day   = now.get(Calendar.DAY_OF_MONTH)

            val picker = DatePickerDialog(
                this,
                { _, sYear, sMonth, sDay ->
                    val sel = Calendar.getInstance().apply {
                        set(sYear, sMonth, sDay)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val tomorrow = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (sel.before(tomorrow)) {
                        Toast.makeText(
                            this,
                            "Please select a date at least 1 day in advance",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DatePickerDialog
                    }
                    calendar.set(sYear, sMonth, sDay)
                    etDate.setText(
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(calendar.time)
                    )
                    checkAndShowTimeSlotsWithRestDayCheck()
                },
                year, month, day
            )

            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
            picker.datePicker.minDate = tomorrow.timeInMillis
            picker.show()
        }
    }

    // Form validation
    private fun setupFormValidation() {
        etUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkAndShowTimeSlotsWithRestDayCheck()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun checkAndShowTimeSlotsWithRestDayCheck() {
        val ok = etUsername.text.isNotEmpty() &&
                spinnerBarber.selectedItemPosition > 0 &&
                spinnerService.selectedItemPosition > 0 &&
                etDate.text.isNotEmpty()

        if (!ok) {
            hideTimeSlots()
            return
        }

        val barberName = spinnerBarber.selectedItem?.toString() ?: run {
            hideTimeSlots()
            return
        }
        val dateStr = etDate.text.toString().trim()

        try {
            val parsed = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(dateStr)
                ?: run { showTimeSlots(); return }
            val dayAbbr = SimpleDateFormat("EEE", Locale.ENGLISH).format(parsed)

            val db = Database(this)
            val restDays = try {
                db.getRestDays(barberName)
            } catch (e: Exception) {
                emptyList()
            } finally {
                db.close()
            }

            if (restDays.contains(dayAbbr)) {
                hideTimeSlots()
                tvTimeSlotNote.visibility = View.VISIBLE
                tvTimeSlotNote.text =
                    "$barberName is off on ${dayAbbr}s. Please choose a different date."
            } else {
                tvTimeSlotNote.text = "Please select a barber and date first"
                showTimeSlots()
            }
        } catch (e: Exception) {
            showTimeSlots()
        }
    }

    // Time slots
    private fun showTimeSlots() {
        tvTimeSlotNote.visibility = View.GONE

        val parentLayout = tvTimeSlotNote.parent as? LinearLayout ?: return
        var container    = parentLayout.findViewWithTag<LinearLayout>("timeSlotContainer")

        if (container == null) {
            container = LinearLayout(this).apply {
                tag         = "timeSlotContainer"
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, dpToPx(8), 0, dpToPx(16)) }
            }
            parentLayout.addView(
                container,
                parentLayout.indexOfChild(tvTimeSlotNote) + 1
            )
        }

        container.removeAllViews()
        container.visibility = View.VISIBLE

        val barberName = spinnerBarber.selectedItem?.toString() ?: return
        val dateStr    = etDate.text.toString().trim()

        val bookedSlots = try {
            val db    = Database(this)
            val slots = db.getBookedSlots(barberName, dateStr)
            db.close()
            slots
        } catch (e: Exception) {
            emptyList()
        }

        val isDark        = DarkModeHelper.isDarkMode(this)
        val bgAvailable   = if (isDark) Color.parseColor("#2C2C2C") else Color.WHITE
        val bgBooked      = if (isDark) Color.parseColor("#1A1A1A") else Color.parseColor("#DDDDDD")
        val textAvailable = if (isDark) Color.WHITE                 else Color.BLACK
        val textBooked    = if (isDark) Color.parseColor("#666666") else Color.parseColor("#999999")

        val timeSlots = generateTimeSlots()
        timeSlots.forEachIndexed { index, timeSlot ->
            if (index % 2 == 0) {
                container.addView(LinearLayout(this).apply {
                    orientation  = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 0, 0, dpToPx(8)) }
                })
            }
            val rowLayout = container.getChildAt(container.childCount - 1) as? LinearLayout ?: return
            val isBooked  = bookedSlots.contains(timeSlot)

            rowLayout.addView(Button(this).apply {
                text      = if (isBooked) "$timeSlot\n(Booked)" else timeSlot
                isEnabled = !isBooked
                textSize  = 12f
                setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(16))
                setBackgroundColor(if (isBooked) bgBooked   else bgAvailable)
                setTextColor      (if (isBooked) textBooked else textAvailable)
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).also {
                    if (index % 2 == 0) it.setMargins(0, 0, dpToPx(4), 0)
                    else                it.setMargins(dpToPx(4), 0, 0, 0)
                }
                if (!isBooked) {
                    setOnClickListener {
                        selectedTimeSlot = timeSlot
                        updateTimeSlotButtons(container, this)
                        scrollToBottom()
                    }
                }
            })
        }
        tvTimeSlotNote.postDelayed({ scrollToBottom() }, 100)
    }

    private fun updateTimeSlotButtons(container: LinearLayout, selected: Button) {
        val isDark        = DarkModeHelper.isDarkMode(this)
        val bgAvailable   = if (isDark) Color.parseColor("#2C2C2C") else Color.WHITE
        val textAvailable = if (isDark) Color.WHITE                 else Color.BLACK

        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as? LinearLayout)?.let { row ->
                for (j in 0 until row.childCount) {
                    (row.getChildAt(j) as? Button)?.let { btn ->
                        if (btn.isEnabled) {
                            btn.setBackgroundColor(bgAvailable)
                            btn.setTextColor(textAvailable)
                        }
                    }
                }
            }
        }
        selected.setBackgroundColor(Color.parseColor("#6366F1"))
        selected.setTextColor(Color.WHITE)
    }

    private fun scrollToBottom() {
        var current: ViewParent? = tvTimeSlotNote.parent
        while (current != null) {
            val node = current
            if (node is ScrollView) {
                node.postDelayed({ node.fullScroll(View.FOCUS_DOWN) }, 100)
                break
            }
            current = node.parent
        }
    }

    private fun generateTimeSlots(): List<String> = (9 until 18).map { hour ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        SimpleDateFormat("h:00 a", Locale.getDefault()).format(cal.time)
    }

    private fun hideTimeSlots() {
        tvTimeSlotNote.visibility = View.VISIBLE
        tvTimeSlotNote.text       = "Please select a barber and date first"
        (tvTimeSlotNote.parent as? LinearLayout)
            ?.findViewWithTag<LinearLayout>("timeSlotContainer")
            ?.visibility = View.GONE
        selectedTimeSlot = null
    }

    // Misc helpers
    private fun updateTotalAmount() {
        tvTotalAmount.text = "₱${String.format("%.2f", selectedServicePrice)}"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun updateDarkModeButton() {
        btnDarkMode.text = if (DarkModeHelper.isDarkMode(this)) "Light Mode" else "Dark Mode"
    }
}
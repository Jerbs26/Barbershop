package com.example.barbershop.user

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.barbershop.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

@ExperimentalGetImage
class FaceShapeActivity : AppCompatActivity() {

    // Camera preview and UI controls
    private lateinit var previewView      : PreviewView
    private lateinit var tvInstruction    : TextView
    private lateinit var tvFaceDetected   : TextView
    private lateinit var btnCapture       : Button
    private lateinit var btnRetake        : Button
    private lateinit var btnFlipCamera    : Button
    private lateinit var btnBack          : ImageButton
    private lateinit var btnBookFromResult: Button
    private lateinit var resultsPanel     : ScrollView
    private lateinit var captureRow       : LinearLayout

    // Result panel views that display the detected face shape and haircut suggestions
    private lateinit var tvFaceShapeEmoji : TextView
    private lateinit var tvFaceShapeResult: TextView
    private lateinit var tvFaceShapeDesc  : TextView
    private lateinit var tvHaircut1Name   : TextView
    private lateinit var tvHaircut1Desc   : TextView
    private lateinit var tvHaircut1Badge  : TextView
    private lateinit var tvHaircut2Name   : TextView
    private lateinit var tvHaircut2Desc   : TextView
    private lateinit var tvHaircut3Name   : TextView
    private lateinit var tvHaircut3Desc   : TextView

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture  : ImageCapture? = null

    // Single background thread for all camera image analysis work
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Tracks whether to use front or back camera — defaults to front for selfie use
    private var useFrontCamera = true

    // True when the detected face is properly centered inside the oval guide
    private var faceInsideOval = false

    // ML Kit face detector — initialized lazily so it's only created when first needed
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    // Simple data holders for haircut suggestions and the full face shape info block
    data class HaircutSuggestion(val name: String, val desc: String)
    data class FaceShapeInfo(
        val shape      : String,
        val emoji      : String,
        val description: String,
        val haircuts   : List<HaircutSuggestion>
    )

    // Lookup table that maps each face shape to its description and top 5 haircut recommendations
    private val faceShapeMap = mapOf(

        "Oval" to FaceShapeInfo(
            shape = "Oval", emoji = "",
            description = "Well-balanced proportions — the most versatile face shape. Almost any style works, so go bold or classic.",
            haircuts = listOf(
                HaircutSuggestion("Pompadour",  "Swept-back volume on top — shows off your balanced features"),
                HaircutSuggestion("Quiff",      "Forward-lifted texture that highlights your symmetry"),
                HaircutSuggestion("Slick Back", "Sleek and polished — your proportions carry it perfectly"),
                HaircutSuggestion("Top Knot",   "Long hair tied up — works great with your balanced shape"),
                HaircutSuggestion("Bro Flow",   "Natural flowing length — effortlessly suits oval faces")
            )
        ),

        "Round" to FaceShapeInfo(
            shape = "Round", emoji = "",
            description = "Full cheeks and soft angles — go for height on top and tight sides to elongate your face.",
            haircuts = listOf(
                HaircutSuggestion("High Fade",  "Tight sides with height on top slims the face visually"),
                HaircutSuggestion("Faux Hawk",  "Center height creates a strong angular illusion"),
                HaircutSuggestion("Mohawk",     "Bold strip of height dramatically elongates round faces"),
                HaircutSuggestion("Undercut",   "Shaved sides with length on top adds vertical emphasis"),
                HaircutSuggestion("Mid Fade",   "Balanced fade that draws the eye upward")
            )
        ),

        "Square" to FaceShapeInfo(
            shape = "Square", emoji = "",
            description = "Strong jawline and wide forehead — soften sharp angles with texture, layers, and soft fades.",
            haircuts = listOf(
                HaircutSuggestion("Textured Crop", "Choppy texture softens the strong angular jaw"),
                HaircutSuggestion("Ivy League",    "Classic side part that polishes and softens sharp lines"),
                HaircutSuggestion("Taper Fade",    "Gradual fade blends the sides smoothly"),
                HaircutSuggestion("Low Fade",      "Subtle fade keeps the jaw from looking too harsh"),
                HaircutSuggestion("Crew Cut",      "Short and neat — balances the strong square structure")
            )
        ),

        "Oblong" to FaceShapeInfo(
            shape = "Oblong", emoji = "",
            description = "Long and narrow face — add width on the sides and keep the top short to balance proportions.",
            haircuts = listOf(
                HaircutSuggestion("Caesar Cut",  "Horizontal fringe shortens and widens the face"),
                HaircutSuggestion("French Crop", "Textured fringe with side volume adds needed width"),
                HaircutSuggestion("Buzz Cut",    "Even all-around length balances a long face perfectly"),
                HaircutSuggestion("Side Part",   "Side-swept style adds width without extra height"),
                HaircutSuggestion("Comb Over",   "Swept to the side, it broadens the visual width")
            )
        ),

        "Heart" to FaceShapeInfo(
            shape = "Heart", emoji = "",
            description = "Wide forehead with a narrow chin — reduce the forehead's width and add fullness near the jaw.",
            haircuts = listOf(
                HaircutSuggestion("Man Bun",    "Sweeps hair back and up, balancing the wide forehead"),
                HaircutSuggestion("Mullet",     "Short top with length at the back adds jaw-level volume"),
                HaircutSuggestion("Low Fade",   "Keeps sides fuller near the jaw to balance the chin"),
                HaircutSuggestion("Taper Fade", "Gradual taper maintains fullness at the right level"),
                HaircutSuggestion("Bro Flow",   "Side-swept flow reduces forehead emphasis naturally")
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_face_shape)
            bindViews()
            setupClickListeners()
            // Ask for camera permission first if we don't already have it
            if (hasCameraPermission()) startCamera()
            else ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
            )
        } catch (e: Exception) {
            // Catch any crash during setup and exit gracefully rather than showing a white screen
            Log.e(TAG, "onCreate crash: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Shut down the background camera thread and release the face detector when the screen closes
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        try { faceDetector.close() } catch (e: Exception) { /* ignore */ }
    }

    // Map all layout views to their member variables in one place
    private fun bindViews() {
        previewView       = findViewById(R.id.previewView)
        tvInstruction     = findViewById(R.id.tvInstruction)
        tvFaceDetected    = findViewById(R.id.tvFaceDetected)
        btnCapture        = findViewById(R.id.btnCapture)
        btnRetake         = findViewById(R.id.btnRetake)
        btnFlipCamera     = findViewById(R.id.btnFlipCamera)
        btnBack           = findViewById(R.id.btnBack)
        btnBookFromResult = findViewById(R.id.btnBookFromResult)
        resultsPanel      = findViewById(R.id.resultsPanel)
        captureRow        = findViewById(R.id.captureRow)

        tvFaceShapeEmoji  = findViewById(R.id.tvFaceShapeEmoji)
        tvFaceShapeResult = findViewById(R.id.tvFaceShapeResult)
        tvFaceShapeDesc   = findViewById(R.id.tvFaceShapeDesc)
        tvHaircut1Name    = findViewById(R.id.tvHaircut1Name)
        tvHaircut1Desc    = findViewById(R.id.tvHaircut1Desc)
        tvHaircut1Badge   = findViewById(R.id.tvHaircut1Badge)
        tvHaircut2Name    = findViewById(R.id.tvHaircut2Name)
        tvHaircut2Desc    = findViewById(R.id.tvHaircut2Desc)
        tvHaircut3Name    = findViewById(R.id.tvHaircut3Name)
        tvHaircut3Desc    = findViewById(R.id.tvHaircut3Desc)
    }

    // Wire up all button click listeners and set the initial capture button state
    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        // Flip toggles the camera selector and restarts the camera feed
        btnFlipCamera.setOnClickListener { useFrontCamera = !useFrontCamera; startCamera() }

        // Capture starts disabled — it only enables once a face is properly centered in the oval
        btnCapture.isEnabled = false
        btnCapture.alpha = 0.4f
        btnCapture.setOnClickListener { captureAndAnalyze() }

        // Retake resets the UI back to camera mode so the user can try again
        btnRetake.setOnClickListener { showCameraMode() }

        // Book button just closes this screen and returns to wherever launched it
        btnBookFromResult.setOnClickListener { finish() }
    }

    // Initializes the camera, sets up preview, image capture, and live face detection analysis
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Preview feeds the viewfinder on screen
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // ImageCapture is used when the user taps the capture button
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // ImageAnalysis runs live face detection on every incoming frame
                // STRATEGY_KEEP_ONLY_LATEST drops old frames if analysis falls behind
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(cameraExecutor) @ExperimentalGetImage { imageProxy ->
                            detectFaceLive(imageProxy)
                        }
                    }

                val cameraSelector = if (useFrontCamera)
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind everything first to avoid "already bound" errors when flipping camera
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis)

            } catch (e: Exception) {
                Log.e(TAG, "Camera start error: ${e.message}", e)
                runOnUiThread { Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Runs on every camera frame — checks if a face is detected and centered inside the oval guide
    @ExperimentalGetImage
    private fun detectFaceLive(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                runOnUiThread {
                    if (faces.isNotEmpty()) {
                        faceInsideOval = isFaceInsideOval(faces.first(), imageProxy.width, imageProxy.height)
                        if (faceInsideOval) {
                            // Face is in position — enable capture and give the user the go-ahead
                            tvFaceDetected.visibility = View.VISIBLE
                            tvFaceDetected.text       = "● Face Detected"
                            tvFaceDetected.setTextColor(0xFF4CAF50.toInt())
                            tvInstruction.text        = "Perfect! Tap 📸 to capture"
                            btnCapture.isEnabled      = true
                            btnCapture.alpha          = 1.0f
                        } else {
                            // Face is too far off-center or too small — nudge the user to adjust
                            tvFaceDetected.visibility = View.VISIBLE
                            tvFaceDetected.text       = "Move closer / center your face"
                            tvFaceDetected.setTextColor(0xFFFF9800.toInt())
                            tvInstruction.text        = "Align your face inside the oval guide"
                            btnCapture.isEnabled      = false
                            btnCapture.alpha          = 0.4f
                        }
                    } else {
                        // No face in frame at all — hide the indicator and disable capture
                        faceInsideOval = false
                        tvFaceDetected.visibility = View.GONE
                        tvInstruction.text = "No face detected — position your face inside the oval"
                        btnCapture.isEnabled = false
                        btnCapture.alpha = 0.4f
                    }
                }
            }
            .addOnFailureListener { Log.e(TAG, "Live detection error: ${it.message}") }
            // Always close the proxy so the camera pipeline doesn't stall
            .addOnCompleteListener { imageProxy.close() }
    }

    // Takes a still photo and runs the full face shape analysis on it
    private fun captureAndAnalyze() {
        val capture = imageCapture ?: return
        btnCapture.isEnabled = false
        tvInstruction.text = "Analyzing your face shape…"

        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @ExperimentalGetImage
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        runOnUiThread { btnCapture.isEnabled = true }
                        return
                    }

                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            imageProxy.close()
                            if (faces.isEmpty()) {
                                // Somehow no face in the still shot — ask the user to try again
                                runOnUiThread {
                                    Toast.makeText(this@FaceShapeActivity, "No face detected. Please try again.", Toast.LENGTH_SHORT).show()
                                    btnCapture.isEnabled = true
                                    tvInstruction.text = "Position your face inside the oval"
                                }
                            } else {
                                // Got a face — determine its shape and show the results panel
                                val shape = determineFaceShape(faces.first())
                                runOnUiThread { showResults(shape) }
                            }
                        }
                        .addOnFailureListener {
                            imageProxy.close()
                            runOnUiThread {
                                Toast.makeText(this@FaceShapeActivity, "Detection failed. Please try again.", Toast.LENGTH_SHORT).show()
                                btnCapture.isEnabled = true
                            }
                        }
                }

                // Hardware or system error during capture — just re-enable the button
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture error: ${exception.message}")
                    runOnUiThread { btnCapture.isEnabled = true }
                }
            }
        )
    }

    // Checks whether the detected face is roughly centered and properly sized inside the oval overlay
    // Uses normalized coordinates so the check works regardless of camera resolution
    private fun isFaceInsideOval(face: Face, imgWidth: Int, imgHeight: Int): Boolean {
        if (imgWidth == 0 || imgHeight == 0) return false
        val box = face.boundingBox
        val w = imgWidth.toFloat()
        val h = imgHeight.toFloat()

        // Normalize face position and size to 0–1 range
        val faceCenterX     = box.exactCenterX() / w
        val faceCenterY     = box.exactCenterY() / h
        val faceWidthRatio  = box.width().toFloat()  / w
        val faceHeightRatio = box.height().toFloat() / h

        // Face center must be within the middle 40% of the frame horizontally and vertically
        val centerOk = faceCenterX in 0.30f..0.70f && faceCenterY in 0.25f..0.75f

        // Face must be large enough to analyze but not so large it's cropped
        val maxRatio = maxOf(faceWidthRatio, faceHeightRatio)
        val minRatio = minOf(faceWidthRatio, faceHeightRatio)
        val sizeOk   = minRatio >= 0.25f && maxRatio <= 0.90f

        return centerOk && sizeOk
    }

    // The main face shape classification logic — uses ML Kit face contour points when available
    // Falls back to bounding box ratio if contour data is missing or unreliable
    private fun determineFaceShape(face: Face): String {
        val contourPoints = face.getContour(FaceContour.FACE)?.points

        if (!contourPoints.isNullOrEmpty()) {
            // Sort points to get overall face bounds
            val sortedByY = contourPoints.sortedBy { it.y }
            val sortedByX = contourPoints.sortedBy { it.x }

            val faceHeight = sortedByY.last().y - sortedByY.first().y
            val faceWidth  = sortedByX.last().x - sortedByX.first().x

            // If contour data is degenerate, fall back to bounding box method
            if (faceHeight <= 0f || faceWidth <= 0f) return fallbackShape(face)

            // Measure jaw width using the bottom 20% of contour points
            val jawThreshold = sortedByY.last().y - (faceHeight * 0.20f)
            val jawPoints    = contourPoints.filter { it.y >= jawThreshold }
            val jawWidth     = if (jawPoints.size >= 2)
                jawPoints.maxOf { it.x } - jawPoints.minOf { it.x }
            else faceWidth * 0.75f   // safe fallback if not enough jaw points

            // Measure forehead width using the top 20% of contour points
            val foreheadThreshold = sortedByY.first().y + (faceHeight * 0.20f)
            val foreheadPoints    = contourPoints.filter { it.y <= foreheadThreshold }
            val foreheadWidth     = if (foreheadPoints.size >= 2)
                foreheadPoints.maxOf { it.x } - foreheadPoints.minOf { it.x }
            else faceWidth * 0.85f

            // Measure cheekbone width around the vertical midpoint of the face
            val midY        = sortedByY.first().y + faceHeight * 0.5f
            val cheekPoints = contourPoints.filter { it.y in (midY - faceHeight * 0.10f)..(midY + faceHeight * 0.10f) }
            val cheekWidth  = if (cheekPoints.size >= 2)
                cheekPoints.maxOf { it.x } - cheekPoints.minOf { it.x }
            else faceWidth

            // Key ratios used for shape classification
            val heightToWidth  = faceHeight / faceWidth         // > 1 means taller than wide
            val jawToFace      = jawWidth    / faceWidth         // how wide the jaw is relative to full face
            val foreheadToJaw  = if (jawWidth > 0f) foreheadWidth / jawWidth else 1f
            val cheekToJaw     = if (jawWidth > 0f) cheekWidth   / jawWidth  else 1f

            Log.d(TAG, "h/w=$heightToWidth jaw/face=$jawToFace fhd/jaw=$foreheadToJaw chk/jaw=$cheekToJaw")

            return when {
                // Very tall relative to width — long and narrow face
                heightToWidth > 1.25f -> "Oblong"

                // Nearly as wide as tall with a wide jaw — full circular shape
                heightToWidth < 1.05f && jawToFace > 0.70f -> "Round"

                // Moderate height with a wide jaw — strong boxy structure
                heightToWidth in 1.05f..1.20f && jawToFace > 0.75f -> "Square"

                // Forehead clearly wider than jaw — wider at top, narrower at chin
                foreheadToJaw > 1.20f && heightToWidth < 1.30f -> "Heart"

                // Balanced proportions that don't fit any specific category
                else -> "Oval"
            }
        }

        // No usable contour data — fall back to the simpler bounding box method
        return fallbackShape(face)
    }

    // Backup shape detection using just the face bounding box dimensions
    // Less accurate than contour analysis but works when contour data isn't available
    private fun fallbackShape(face: Face): String {
        val box    = face.boundingBox
        val eulerZ = face.headEulerAngleZ

        val rawW = box.width().toFloat()
        val rawH = box.height().toFloat()

        // Swap width and height if the head is tilted significantly — prevents wrong ratios
        val (faceW, faceH) = if (rawW > rawH && abs(eulerZ) < 30f)
            Pair(rawH, rawW) else Pair(rawW, rawH)

        if (faceW == 0f) return "Oval"

        val ratio = faceH / faceW

        Log.d(TAG, "Fallback ratio=$ratio eulerZ=$eulerZ")

        return when {
            ratio > 1.25f -> "Oblong"
            ratio < 1.05f -> "Round"
            ratio < 1.15f -> "Square"
            ratio > 1.15f -> "Heart"
            else           -> "Oval"
        }
    }

    // Populates the results panel with the detected face shape info and haircut suggestions
    // Also animates the panel into view and hides the capture button
    private fun showResults(shape: String) {
        // Fall back to Oval info if somehow the shape isn't in the map
        val info = faceShapeMap[shape] ?: faceShapeMap["Oval"]!!

        tvFaceShapeEmoji.text  = info.emoji
        tvFaceShapeResult.text = info.shape
        tvFaceShapeDesc.text   = info.description

        val haircuts = info.haircuts

        // Fill the first 3 haircut slots that are hardcoded in the layout
        tvHaircut1Name.text        = haircuts.getOrNull(0)?.name ?: ""
        tvHaircut1Desc.text        = haircuts.getOrNull(0)?.desc ?: ""
        tvHaircut1Badge.visibility = if (haircuts.isNotEmpty()) View.VISIBLE else View.GONE
        tvHaircut2Name.text        = haircuts.getOrNull(1)?.name ?: ""
        tvHaircut2Desc.text        = haircuts.getOrNull(1)?.desc ?: ""
        tvHaircut3Name.text        = haircuts.getOrNull(2)?.name ?: ""
        tvHaircut3Desc.text        = haircuts.getOrNull(2)?.desc ?: ""

        // Haircuts 4 and 5 are added dynamically since they don't have hardcoded layout slots
        val resultsContent = resultsPanel.getChildAt(0) as? LinearLayout
        addDynamicHaircutCard(resultsContent, "slot4", haircuts.getOrNull(3))
        addDynamicHaircutCard(resultsContent, "slot5", haircuts.getOrNull(4))

        // Fade the results panel in smoothly
        resultsPanel.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(resultsPanel, "alpha", 0f, 1f).apply {
            duration = 400; start()
        }

        // Expand the results panel to take up more vertical space
        val params = resultsPanel.layoutParams as LinearLayout.LayoutParams
        params.weight = 1.5f
        resultsPanel.layoutParams = params

        // Switch the action buttons — hide capture, show retake
        btnCapture.visibility     = View.GONE
        btnRetake.visibility      = View.VISIBLE
        tvInstruction.text        = "Here are your best haircut matches!"
        tvFaceDetected.visibility = View.GONE
    }

    // Adds or updates a dynamically created haircut card in the results panel
    // Uses a string tag to find and reuse existing cards instead of creating duplicates
    private fun addDynamicHaircutCard(parent: LinearLayout?, tag: String, haircut: HaircutSuggestion?) {
        if (parent == null) return

        // Check if a card for this slot already exists from a previous result
        val existing = parent.findViewWithTag<LinearLayout>(tag)
        if (existing != null) {
            if (haircut == null) { existing.visibility = View.GONE; return }
            // Reuse the existing card — just update its text
            (existing.getChildAt(0) as? TextView)?.text = haircut.name
            (existing.getChildAt(1) as? TextView)?.text = haircut.desc
            existing.visibility = View.VISIBLE
            return
        }

        // No existing card for this slot and no haircut to show — nothing to do
        if (haircut == null) return

        // Build a new card from scratch and add it to the results container
        val card = LinearLayout(this).apply {
            this.tag    = tag
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            setBackgroundColor(Color.parseColor("#F9F9F9"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, dpToPx(8), 0, 0) }
        }

        // Haircut name — bold and larger
        card.addView(TextView(this).apply {
            text     = haircut.name
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#2C2C2C"))
        })

        // Haircut description — smaller and muted
        card.addView(TextView(this).apply {
            text     = haircut.desc
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(4) }
        })

        parent.addView(card)
    }

    // Converts dp to pixels using the device's screen density
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // Resets the UI back to live camera mode so the user can try a different angle or retake
    private fun showCameraMode() {
        resultsPanel.visibility   = View.GONE
        btnCapture.visibility     = View.VISIBLE
        btnCapture.isEnabled      = false
        btnCapture.alpha          = 0.4f
        btnRetake.visibility      = View.GONE
        tvFaceDetected.visibility = View.GONE
        tvInstruction.text        = "Position your face inside the oval"

        // Collapse the results panel weight back to 0 so the camera preview takes full space again
        val params = resultsPanel.layoutParams as LinearLayout.LayoutParams
        params.weight = 0f
        resultsPanel.layoutParams = params
    }

    // Simple check to see if the camera permission has already been granted
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    // Handles the result of the camera permission dialog
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startCamera()
            else {
                // Permission denied — camera is required so we can't continue
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "FaceShapeActivity"
        private const val REQUEST_CAMERA_PERMISSION = 101
    }
}
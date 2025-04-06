package com.example.buhackaccino

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.camera.core.Camera
import android.speech.tts.TextToSpeech
import java.io.ByteArrayOutputStream
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import com.android.volley.DefaultRetryPolicy
import com.example.buhackaccino.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import android.speech.tts.UtteranceProgressListener

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var lastImageUrl: String? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech
    private lateinit var binding: ActivityMainBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private val NEBIUS_API_ENDPOINT = "https://api.studio.nebius.com/v1/chat/completions"
    private val NEBIUS_API_KEY = "eyJhbGciOiJIUzI1NiIsImtpZCI6IlV6SXJWd1h0dnprLVRvdzlLZWstc0M1akptWXBvX1VaVkxUZlpnMDRlOFUiLCJ0eXAiOiJKV1QifQ.eyJzdWIiOiJnb29nbGUtb2F1dGgyfDExNDY0MDczMjM5MDk5ODIyOTYwMiIsInNjb3BlIjoib3BlbmlkIG9mZmxpbmVfYWNjZXNzIiwiaXNzIjoiYXBpX2tleV9pc3N1ZXIiLCJhdWQiOlsiaHR0cHM6Ly9uZWJpdXMtaW5mZXJlbmNlLmV1LmF1dGgwLmNvbS9hcGkvdjIvIl0sImV4cCI6MTkwMTUyMDk2MCwidXVpZCI6ImQxMmIwYTMzLWVmMmEtNGViNC1hMTA5LTk4OWUyMWIxMTRjZSIsIm5hbWUiOiJURVNUMSIsImV4cGlyZXNfYXQiOiIyMDMwLTA0LTA0VDA4OjE2OjAwKzAwMDAifQ.tmdbnrAUQmsopIDMYCmxI16HpG5Ij8dijxfFKlThHrw"
    private var isCameraFrozen = false
    private lateinit var chatAdapter: ChatAdapter
    private var startX = 0f
    private var startY = 0f
    private val SWIPE_MIN_DISTANCE = 120
    private var touchStartTime: Long = 0
    private val MAX_CLICK_DURATION = 200
    private var isUserScrolling = false
    private lateinit var translator: Translator
    private var selectedLanguage = "en"
    private lateinit var selectedLocale: Locale
    private var isTTSInitialized = false
    private var pendingTextToSpeak: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Get language from intent
        selectedLanguage = intent.getStringExtra("selected_language") ?: "en"
        selectedLocale = when (selectedLanguage) {
            "hi" -> Locale("hi", "IN")
            "ta" -> Locale("ta", "IN")
            "bn" -> Locale("bn", "IN")
            "te" -> Locale("te", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "kn" -> Locale("kn", "IN")
            "ur" -> Locale("ur", "IN")
            else -> Locale.US
        }

        // Setup translator
        setupTranslator()



        binding.progressBarCard.visibility = View.GONE
        // Initialize chat recycler view
        chatAdapter = ChatAdapter(this)
        // Add in onCreate after chatAdapter initialization
        binding.promptInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendCustomPrompt()
                true
            } else {
                false
            }
        }
        binding.btnAdd.setOnClickListener {
            sendCustomPrompt()
        }
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true // Makes the list stack from bottom
                reverseLayout = false
            }
            adapter = chatAdapter
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        binding.chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                isUserScrolling = newState == RecyclerView.SCROLL_STATE_DRAGGING
            }
        })

        // Initialize Firebase Storage with correct bucket
        try {
            FirebaseApp.initializeApp(this)
            storage = FirebaseStorage.getInstance("gs://buhackaccino-23f99.firebasestorage.app")
            storageRef = storage.reference
            Log.d(TAG, "Storage bucket: ${storage.reference.bucket}")

            // Create images directory if it doesn't exist
            val imagesRef = storageRef.child("images")
            Log.d("meriMarzi", "${storageRef}")
            imagesRef.child(".keep").putBytes(ByteArray(0))
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create images directory: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        tts = TextToSpeech(this, this)
        setupTouchListener()
        cameraExecutor = Executors.newSingleThreadExecutor()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTTSInitialized = true
                val result = tts.setLanguage(selectedLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported: $selectedLanguage")
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                }
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun setupTranslator() {
        if (selectedLanguage == "en") return

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(selectedLanguage)
            .build()

        translator = Translation.getClient(options)

        // Force download model
        binding.progressBar.visibility = View.VISIBLE
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                Log.d(TAG, "Translation model downloaded successfully")
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error downloading translation model: ${exception.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to download translation model", Toast.LENGTH_SHORT).show()
            }
    }

    private fun translateAndAddMessage(englishText: String) {
        if (selectedLanguage == "en") {
            chatAdapter.addMessage(ChatMessage(englishText))
            speakText(englishText)
            return
        }

        translator.translate(englishText)
            .addOnSuccessListener { translatedText ->
                chatAdapter.addMessage(ChatMessage(translatedText))
                speakText(translatedText)
                if (!isUserScrolling) {
                    binding.chatRecyclerView.postDelayed({
                        binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }, 100)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Translation failed: ${exception.message}")
                // Fallback to English
                chatAdapter.addMessage(ChatMessage(englishText))
                speakText(englishText)
            }
    }

    private fun setupTouchListener() {
        binding.viewFinder.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.x
                    val endY = event.y
                    val touchDuration = System.currentTimeMillis() - touchStartTime

                    val distanceX = abs(endX - startX)
                    val distanceY = abs(endY - startY)

                    if (touchDuration < MAX_CLICK_DURATION && distanceX < 50 && distanceY < 50) {
                        freezeCamera()
                        takePhoto()
                    } else if (distanceX > SWIPE_MIN_DISTANCE && distanceX > distanceY) {
                        if (startX > endX) {
                            startSecondActivity()
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }
    private fun sendCustomPrompt() {
        lastImageUrl?.let { url ->
            val customPrompt = binding.promptInput.text.toString().trim()
            if (customPrompt.isNotEmpty()) {
                sendImageUrlToApi(url, customPrompt)
                binding.promptInput.text.clear()
                hideKeyboard()
            }
        }
    }

    private fun uploadImageToFirebase(imageBytes: ByteArray) {
        binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Processing image, please wait...", Toast.LENGTH_SHORT).show()
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "image_${timestamp}.jpg"
            val imageRef = storageRef.child("images/$fileName")

            Log.d(TAG, "Starting upload to: ${imageRef.path}")

            // Create metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Start upload
            val uploadTask = imageRef.putBytes(imageBytes, metadata)

            uploadTask
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    Log.d(TAG, "Upload progress: $progress%")
                }
                .addOnSuccessListener {
                    // Get HTTPS download URL after successful upload
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            val httpsUrl = downloadUri.toString()
                            Log.d(TAG, "HTTPS Download URL: $httpsUrl")
                            sendImageUrlToApi(httpsUrl)
                        }
                        .addOnFailureListener { e ->
                            val errorMsg = "Failed to get download URL: ${e.message}"
                            Log.e(TAG, errorMsg, e)
                            Toast.makeText(baseContext, errorMsg, Toast.LENGTH_LONG).show()
                            resumeCamera()
                        }
                }
                .addOnFailureListener { e ->
                    val errorMsg = "Upload failed: ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    Toast.makeText(baseContext, errorMsg, Toast.LENGTH_LONG).show()
                    resumeCamera()
                }

        } catch (e: Exception) {
            val errorMsg = "Error in upload: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Toast.makeText(baseContext, errorMsg, Toast.LENGTH_LONG).show()
            resumeCamera()
        }
    }
    private fun sendImageUrlToApi(firebaseUrl: String, customPrompt: String = "What's in this image ?") {
        lastImageUrl = firebaseUrl
        chatAdapter.addMessage(ChatMessage("Analyzing image...", firebaseUrl))
        binding.progressBar.visibility = View.VISIBLE

        val queue = Volley.newRequestQueue(this)
        val jsonObject = JSONObject().apply {
            put("model", "Qwen/Qwen2-VL-72B-Instruct")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", customPrompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", firebaseUrl)
                            })
                        })
                    })
                })
            })
            put("max_tokens", 300)
        }

        val jsonRequest = object : JsonObjectRequest(
            Method.POST,
            NEBIUS_API_ENDPOINT,
            jsonObject,
            { response ->
                try {
                    val englishContent = response.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    // Translate English content to selected language
                    if (selectedLanguage != "en") {
                        translator.translate(englishContent)
                            .addOnSuccessListener { translatedText ->
                                runOnUiThread {
                                    binding.progressBar.visibility = View.GONE
                                    chatAdapter.addMessage(ChatMessage(translatedText))
                                    Log.d(TAG, "Translation successful: $translatedText")

                                    // Ensure TTS is ready
                                    if (isTTSInitialized) {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            speakText(translatedText)
                                        }, 1000)
                                    } else {
                                        pendingTextToSpeak = translatedText
                                    }
                                    scrollToBottom()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Translation failed: ${exception.message}")
                                runOnUiThread {
                                    binding.progressBar.visibility = View.GONE
                                    chatAdapter.addMessage(ChatMessage(englishContent))
                                    speakText(englishContent)
                                    Toast.makeText(baseContext, "Translation failed, showing English text", Toast.LENGTH_SHORT).show()
                                    scrollToBottom()
                                }
                            }
                    } else {
                        runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            chatAdapter.addMessage(ChatMessage(englishContent))
                            speakText(englishContent)
                            scrollToBottom()
                        }
                    }
                    resumeCamera()
                } catch (e: Exception) {
                    handleApiError(e)
                }
            },
            { error -> handleApiError(error) }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return HashMap<String, String>().apply {
                    put("Authorization", "Bearer $NEBIUS_API_KEY")
                    put("Content-Type", "application/json")
                }
            }
        }

        jsonRequest.retryPolicy = DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(jsonRequest)
    }

    private fun scrollToBottom() {
        if (!isUserScrolling) {
            binding.chatRecyclerView.postDelayed({
                binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }, 100)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.promptInput.windowToken, 0)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        // Convert ImageProxy to Bitmap
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        // Compress the image
                        val compressedBytes = compressImage(bitmap)

                        // Show frozen frame
                        showFrozenFrame(bitmap)

                        // Log size comparison
                        Log.d(TAG, "Original size: ${bytes.size}, Compressed size: ${compressedBytes.size}")

                        // Upload compressed image
                        uploadImageToFirebase(compressedBytes)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image: ${e.message}", e)
                        Toast.makeText(baseContext, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
                        resumeCamera()
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(baseContext, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    resumeCamera()
                }
            }
        )
    }
    private fun compressImage(bitmap: Bitmap): ByteArray {
        val maxSize = 800 // Reduced from 1024 to 800
        var scaledBitmap = bitmap

        // Scale down the image if it's larger than maxSize
        if (bitmap.width > maxSize || bitmap.height > maxSize) {
            val scaleFactor = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // Compress with lower quality (70% instead of 85%)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

        // Additional compression if size is still large
        var compressQuality = 70
        var streamLength = outputStream.size()

        while (streamLength > 500000 && compressQuality > 30) { // Limit to 500KB
            outputStream.reset()
            compressQuality -= 10
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
            streamLength = outputStream.size()
        }

        return outputStream.toByteArray()
    }

    private fun showFrozenFrame(bitmap: Bitmap) {
        // Remove any existing frozen frame first
        removeFrozenFrame()

        // Create ImageView with the same dimensions as the preview
        val frozenFrameView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.5).toInt() // 50% of screen height
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
            tag = "frozen_frame"
        }

        // Add the frozen frame view
        (binding.viewFinder.parent as ViewGroup).addView(frozenFrameView)
        binding.viewFinder.visibility = View.INVISIBLE
        isCameraFrozen = true
    }
    private fun removeFrozenFrame() {
        try {
            val viewGroup = binding.viewFinder.parent as ViewGroup
            val frozenFrame = viewGroup.findViewWithTag<ImageView>("frozen_frame")
            frozenFrame?.let {
                viewGroup.removeView(it)
            }
            binding.viewFinder.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error removing frozen frame: ${e.message}")
        }
    }

    private fun freezeCamera() {
        isCameraFrozen = true
        camera?.let { camera ->
            try {
                camera.cameraControl.enableTorch(false)
                val previewView = binding.viewFinder
                val bitmap = previewView.bitmap

                bitmap?.let {
                    // Get preview dimensions
                    val params = previewView.layoutParams as ViewGroup.LayoutParams

                    val frozenFrameView = ImageView(this).apply {
                        layoutParams = ViewGroup.LayoutParams(params.width, params.height)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(bitmap)
                        // Set the same position as the preview view
                        x = previewView.x
                        y = previewView.y
                    }

                    (previewView.parent as ViewGroup).addView(frozenFrameView)
                    frozenFrameView.tag = "frozen_frame"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error freezing camera: ${e.message}")
                resumeCamera()
            }
        }
    }

    private fun resumeCamera() {
        if (!isCameraFrozen) return

        isCameraFrozen = false
        removeFrozenFrame()

        try {
            camera?.cameraControl?.enableTorch(false)
            startCamera() // Restart camera preview
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming camera: ${e.message}")
        }
    }
    private fun handleApiError(error: Exception) {
        val errorMessage = when (error) {
            is com.android.volley.NoConnectionError -> "No internet connection"
            is com.android.volley.TimeoutError -> "Request timed out"
            is com.android.volley.ServerError -> {
                val networkResponse = (error as? com.android.volley.ServerError)?.networkResponse
                "Server Error: ${networkResponse?.statusCode}, Data: ${networkResponse?.data?.let { String(it) }}"
            }
            else -> "API Error: ${error.message}"
        }
        Log.e(TAG, "API Error Details: $errorMessage", error)
        Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
        resumeCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview!!, imageCapture!!)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun speakText(text: String) {
        if (!isTTSInitialized) {
            pendingTextToSpeak = text
            Log.d(TAG, "TTS not initialized, saving text for later: $text")
            return
        }

        try {
            // Set language before speaking
            val result = tts.setLanguage(selectedLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported: $selectedLanguage")
                Toast.makeText(this, "Language not supported: $selectedLanguage", Toast.LENGTH_SHORT).show()
                return
            }

            tts.setSpeechRate(0.8f)
            tts.setPitch(1.0f)

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId")

            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageId") == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text")
                Toast.makeText(this, "Error in text-to-speech", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Speaking text: $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS Error: ${e.message}")
            Toast.makeText(this, "TTS Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTTSInitialized = true
            configureTextToSpeech()

            // Speak any pending text
            pendingTextToSpeak?.let {
                speakText(it)
                pendingTextToSpeak = null
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureTextToSpeech() {
        val result = tts.setLanguage(selectedLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language not supported: $selectedLanguage")

            // Install language data
            val installIntent = Intent()
            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            startActivity(installIntent)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String) {
                    // Try setting language again after potential installation
                    runOnUiThread {
                        if (tts.setLanguage(selectedLocale) == TextToSpeech.SUCCESS) {
                            Log.d(TAG, "Language set successfully after installation")
                        }
                    }
                }

                override fun onError(utteranceId: String) {
                    Log.e(TAG, "TTS Error for utterance: $utteranceId")
                }

                override fun onStart(utteranceId: String) {
                    Log.d(TAG, "TTS Started utterance: $utteranceId")
                }
            })
        }
        tts.setPitch(1.0f)
        tts.setSpeechRate(0.8f)
    }

    private fun startSecondActivity() {
        val intent = Intent(this, Navigation::class.java)
        startActivity(intent)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
        if (::translator.isInitialized && selectedLanguage != "en") {
            translator.close()
        }
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf("android.permission.CAMERA")
    }
}
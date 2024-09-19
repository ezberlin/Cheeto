package com.example.cheeto

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cheeto.databinding.ActivityBoardSettingBinding
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BoardSettingActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityBoardSettingBinding
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityBoardSettingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)

                    // Process the captured image with ML Kit
                    output.savedUri?.let { uri ->
                        processImage(uri)
                    }
                }
            }
        )
    }

    private fun processImage(uri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, uri)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    Log.d(TAG, "Text recognized: $resultText")



                    // Show the EditText and Button
                    viewBinding.editText.setText(resultText)
                    viewBinding.editText.visibility = View.VISIBLE
                    viewBinding.saveButton.visibility = View.VISIBLE

                    // Set up the save button click listener
                    viewBinding.saveButton.setOnClickListener {
                        val editedText = viewBinding.editText.text.toString()
                        // Extract numbers and format as JSON
                        val numbers = extractNumbers(editedText)
                        val json = formatAsJson(numbers)

                        // Save JSON locally
                        saveJsonLocally(json)
                        saveText(editedText)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed: ${e.message}", e)
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun extractNumbers(input: String): List<Int> {
        val numbers = input.filter { it in '1'..'9' }.map { it.toString().toInt() }
        return if (numbers.size >= 49) {
            numbers.take(49)
        } else {
            numbers + List(49 - numbers.size) { 1 }
        }
    }

    private fun formatAsJson(numbers: List<Int>): String {
        val jsonArray = numbers.chunked(7)
        return Gson().toJson(jsonArray)
    }

    private fun saveJsonLocally(json: String) {
        val filesDir = filesDir
        val existingFiles = filesDir.listFiles { _, name -> name.startsWith("b") && name.endsWith(".json") }
        val nextIndex = (existingFiles?.mapNotNull { it.name.removePrefix("b").removeSuffix(".json").toIntOrNull() }?.maxOrNull() ?: 0) + 1
        val fileName = "b$nextIndex.json"
        val file = File(filesDir, fileName)
        file.writeText(json)
    }

    private fun saveText(text: String) {
        Log.d(TAG, "Edited text saved: $text")
        Toast.makeText(baseContext, "Edited text saved: $text", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
package com.kahramanai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns // A robust URI/URL validator
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kahramanai.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val TAG = "Kahraman"

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var barcodeScanner: BarcodeScanner? = null

    private lateinit var binding: ActivityMainBinding

    // 1. Initialize the permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission was granted by the user.
                // startCamera()
            } else {
                // Permission was denied.
                Toast.makeText(this, "QR kod okuma için kamera izni gerekmektedir.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This makes your app draw behind the system status/navigation bars.
        enableEdgeToEdge()

        // This is the correct way to set up the layout with View Binding.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // This listener works with enableEdgeToEdge() to add padding so your content doesn't get hidden by the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left + 15, systemBars.top, systemBars.right + 15, systemBars.bottom)
            insets
        }

        // Init camera components
        viewFinder = binding.viewFinder
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Now that 'binding' is initialized, you can add your TextWatcher logic here.
        val shareLinkEditText = binding.textInputShareLink
        //val useShareLinkButton = binding.btnUseShareLink

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput(s.toString())
            }
        }
        shareLinkEditText.addTextChangedListener(textWatcher)

        // QR Scan
        val scanButton = binding.btnScanQrcode
        val iptalQRscan = binding.cancelQrScan

        // Set the click listener on the button
        scanButton.setOnClickListener {
            // When the button is clicked, perform the following actions:

            // 1. Hide the main content container
            binding.buttonPanel.visibility = View.GONE

            // 2. Show the camera container (FrameLayout)
            binding.cameraContainer.visibility = View.VISIBLE
            iptalQRscan.visibility = View.VISIBLE
            binding.overlayView.startOverlay()

            // 3. Call the function to check permission and launch the camera
            requestCameraAndShow()
        }

        // Set the click listener on iptal qr scan
        iptalQRscan.setOnClickListener {
            // Release camera
            //...
            stopScanning()

        }

    }

    private fun startCamera() {
        Log.d(TAG,"Camera started for Receipt capture.")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            this.cameraProvider = cameraProvider

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                binding.overlayView.startOverlay()
                startBarcodeScanner(cameraProvider, cameraSelector, preview)

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    @OptIn(ExperimentalGetImage::class)
    private fun startBarcodeScanner(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector, preview: Preview) {
        val scanner = BarcodeScanning.getClient()
        barcodeScanner = scanner
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            val localScanner = barcodeScanner  // create local immutable reference
            if (localScanner == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            processImageProxy(localScanner, imageProxy)
        }

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {

                        val overlayView = findViewById<QRScanOverlayView>(R.id.overlayView)
                        overlayView.stopOverlay()

                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            parseQRcode(rawValue.toString())
                            handleQRcodeScanned(rawValue.toString())
                        }
                        stopScanning()
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }



    private fun parseQRcode(rawVal: String){
        println("QR code $rawVal");
        //jwt = rawVal;
    }

    private fun handleQRcodeScanned(qrValue: String){
        Log.d(TAG, qrValue)
        //cameraCaptureButton.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure the camera and executor are released
        releaseCamera()
        cameraExecutor.shutdown()
    }

    private fun stopScanning() {
        // Call the comprehensive release function
        releaseCamera()

        // Update UI elements
        binding.cameraContainer.visibility = View.GONE
        binding.cancelQrScan.visibility = View.GONE
        binding.overlayView.stopOverlay()
        binding.buttonPanel.visibility = View.VISIBLE
    }

    private fun releaseCamera() {
        // 1. Clear the analyzer to stop processing frames
        imageAnalysis?.clearAnalyzer()

        // 2. Unbind all use cases from the lifecycle
        // This is the most important step to release the camera.
        cameraProvider?.unbindAll()

        // 3. Close the barcode scanner to release ML Kit resources
        barcodeScanner?.close()
        barcodeScanner = null // Set to null to prevent reuse of a closed scanner

        // 4. Nullify the cameraProvider reference
        cameraProvider = null
    }

    private fun requestCameraAndShow() {
        // 3. Check the current permission status
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // The permission is already granted.
                // You can directly show the camera.
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // The user has previously denied the permission.
                // You should show a dialog explaining why you need the permission.
                // For this example, we'll just re-request. In a real app, show a dialog.
                Toast.makeText(this, "QR kod okuma için kamera izni gerekmektedir.", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // The permission has not been asked for yet.
                // Launch the permission request dialog.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Checks if the input string is a valid web URL and updates the button's visibility.
     */
    private fun validateInput(input: String) {
        // Patterns.WEB_URL is a robust regex for checking if a string is a valid URL.
        if (Patterns.WEB_URL.matcher(input).matches()) {
            // The input is a valid URL, show the button
            binding.btnUseShareLink.visibility = View.VISIBLE
        } else {
            // The input is not a valid URL, hide the button
            binding.btnUseShareLink.visibility = View.GONE
        }
    }
}
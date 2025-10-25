package com.kahramanai

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
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
import com.google.android.material.snackbar.Snackbar
import com.kahramanai.ui.MainViewModel
import androidx.activity.viewModels
import com.kahramanai.util.NetworkResult
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.camera.core.ImageCaptureException
import com.kahramanai.data.ResponseJwtBundle
import com.kahramanai.util.PhotoManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kahramanai.data.PresignedUrlResponse
import com.kahramanai.data.UploadRequest
import com.kahramanai.util.getFileDetailsFromUri
import com.kahramanai.util.getFileFromUri
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.core.net.toUri
import com.kahramanai.data.SelectableItem
import com.kahramanai.data.ShrBundle
import com.kahramanai.util.compressImage
import com.kahramanai.util.deleteFileFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val TAG = "Kahraman"

    private lateinit var prefs: SharedPreferences

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var isScanning = false
    private lateinit var binding: ActivityMainBinding

    private lateinit var outputDirectory: File

    private val viewModel: MainViewModel by viewModels()
    private val LOADING_DIALOG_TAG = "loading_dialog"
    private var bid: Int? = 0
    private var cid: Int? = 0

    private var linkVar = false

    private var aiTokenCount: Int? = 1

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

        prefs = getSharedPreferences("myAppPref", MODE_PRIVATE)

        outputDirectory = getOutputDirectory()

        // --- STEP 3: Observe the pending upload count ---
        val uploadProgressBar = binding.uploadProgressBar
        val uploadText = binding.uploadStatusTextView

        lifecycleScope.launch {
            // This coroutine will automatically restart when the activity is started
            // and stop when the activity is stopped.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pendingUploads.collect { count ->
                    // Update your UI based on the count
                    if (count > 0) {
                        uploadProgressBar.visibility = View.VISIBLE
                        val uploadStatus = "$count belge yükleniyor..."
                        uploadText.text = uploadStatus
                    } else {
                        uploadProgressBar.visibility = View.GONE
                        getUserCredits()
                    }
                }
            }
        }

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

        // Share Link
        val shareLinkButton = binding.btnUseShareLink
        shareLinkButton.setOnClickListener {
            val enteredText = binding.textInputShareLink.text.toString().trim()
            checkShareLink(enteredText)
        }

        // QR Scan
        val scanButton = binding.btnScanQrcode
        val iptalQRscan = binding.cancelQrScan

        // Set the click listener on the button
        scanButton.setOnClickListener {

            isScanning = true // set the camera mode to scan

            // 1. Hide the panels
            visibilityPanelForLinks(false)
            visibilityPanelForKahraman(false)

            // 2. Show the camera container (FrameLayout)
            updateCameraUI(true)

            updateCameraContainerConstraints(R.id.scanViewText, R.id.cancel_qr_scan)

            // 3. Call the function to check permission and launch the camera
            requestCameraAndShow()
        }

        // Set the click listener on iptal qr scan
        iptalQRscan.setOnClickListener {
            stopScanning()
        }

        // Camera iptal
        val iptalButton = binding.cancelPhoto

        iptalButton.setOnClickListener {
            stopCamera()
        }

        // Camera action
        val captureImageButton = binding.cameraButton

        captureImageButton.setOnClickListener {
            takePhoto()
        }

        // Load the last token
        val loadLastToken = binding.btnUseLastLink
        loadLastToken.setOnClickListener {
            val lastToken = prefs.getString("KAI_URL_LINK", "")
            binding.textInputShareLink.setText(lastToken)
            checkShareLink(lastToken.toString())
        }

        // Open the link
        val openLinkTxt = binding.textOpenLink
        openLinkTxt.setOnClickListener {
            val link = binding.textInputShareLink.text.toString().trim()
            openUrlInBrowser(link)
        }

        // Kahramanai.com
        val openKai = binding.txtKaiLink
        openKai.setOnClickListener {
            openUrlInBrowser("https://kahramanai.com")
        }

        // Clear the link text content
        val clearLinkTxt = binding.textClearLink
        clearLinkTxt.setOnClickListener {
            binding.textInputShareLink.setText("")
        }

        // Bundle selection
        supportFragmentManager.setFragmentResultListener(SelectionDialogFragment.REQUEST_KEY, this) { requestKey, bundle ->
            val selectedItemId = bundle.getInt(SelectionDialogFragment.RESULT_KEY)
            bid = selectedItemId
            handleBundleDataComplete()
            val shareToken : String? = prefs.getString("KAI_SHARE_TOKEN", "------")
            Log.d(TAG, "bid: $bid ")
            getBundleData(shareToken, bid!!)
        }

        // Other actions
        checkTheLastLink()
    }

    private fun openUrlInBrowser(url: String) {
        // Create an Intent to view the URL
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())

        try {
            // Start the activity to open the browser
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Error opening URL in browser: ${e.message}")
            // This happens if the user does not have a web browser installed.
            Toast.makeText(this, "Web tarayıcısı bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkShareLink(link: String){

        if(link.length < 100) {
            showSnackbar("Paylaşım linki geçerli değil!")
            return
        }

        val linkJWT = "https://kahramanai.com/jwt/"
        if (link.startsWith(linkJWT, ignoreCase = true)) {
            val jwt: String = link.replaceFirst(linkJWT, "")
            getLinkDataJWT(jwt, link)
            return
        }

        val linkShared = "https://kahramanai.com/shared/"
        if (link.startsWith(linkShared, ignoreCase = true)) {
            val token: String = link.replaceFirst(linkShared, "")
            getLinkDataShared(token, link)
            return
        }

        showSnackbar("Paylaşım linki geçerli değil!")
    }

    // SHARE TOKEN
    private fun getLinkDataShared(shareToken: String, link: String){

        viewModel.routeSharedTokenCheck(shareToken)

        viewModel.postResult5.observe(this) { result ->

            when (result) {
                is NetworkResult.Error<*> -> { dismissLoadingDialog(); showSnackbar("Paylaşım linki geçerli değil!") }
                is NetworkResult.Loading<*> -> { showLoadingDialog() }
                is NetworkResult.Success<*> -> {


                    bid = result.data?.bid
                    cid = result.data?.cid

                    val editor = prefs.edit()
                    editor.putString("KAI_SHARE_TOKEN", shareToken)
                    editor.putString("KAI_URL_LINK", link)
                    editor.putBoolean("KAI_IS_JWT", false)
                    editor.putBoolean("KAI_LINK_VAR", true)
                    editor.apply()
                    linkVar = true
                    //
                    checkTheLastLink()

                    getCompanyData(shareToken)

                    bid?.let {
                        if(it > 0) {
                            handleBundleDataComplete()
                            getBundleData(shareToken, bid!!)
                        } else {
                            handleCompanyShareLink(shareToken)
                        }
                    }
                }
            }
        }
    }

    private fun handleCompanyShareLink (shareToken: String) {

        viewModel.routeSharedListCompanyBundles(shareToken)

        viewModel.postResult10.observe(this) { result ->

            when (result) {
                is NetworkResult.Error<*> -> { showSnackbar("Paylaşım linki geçerli değil!") }
                is NetworkResult.Loading<*> -> { }
                is NetworkResult.Success<*> -> {
                    val bundleList = result.data
                    dismissLoadingDialog()
                    showBundlisList(bundleList)
                }
            }
        }
    }

    private fun showBundlisList (bundleList: List<ShrBundle>?){

        val bundleItems = bundleList
            ?.filter { it.status == 1 }
            ?.map { bundle ->
                val bundleName = "${bundle.bundleCode} / ${bundle.bundleName}"
                SelectableItem(bundle.autoId, bundleName)
            }

        if (bundleItems?.isEmpty() ?: true){
            showSnackbar("Mükellefe ait etiket listesi bulunamadı!")
            return
        }

        if (bundleItems.size == 1){
            val seciliItem: SelectableItem? = bundleItems.firstOrNull()
            if (seciliItem != null){
                bid = seciliItem.id
                handleBundleDataComplete()
                val bundleCodeName = seciliItem.name
                binding.bundleName.text = bundleCodeName
                //getBundleData(shareToken, bid!!)
            }
            return
        }

        val dialogFragment = SelectionDialogFragment.newInstance(bundleItems)

        dialogFragment.show(supportFragmentManager, SelectionDialogFragment.TAG)


    }

    private fun handleBundleDataComplete () {

        visibilityPanelForKahraman(false)
        visibilityPanelForLinks(false)
        visibilityPanelForCompany(true)

        isScanning = false
        updateCameraUI(true)
        requestCameraAndShow()

    }

    private fun getBundleData(shareToken: String?, bid: Int) {

        Log.d(TAG, "Share token: $shareToken")

        viewModel.routeSharedBundleData(shareToken, bid)

        viewModel.postResult7.observe(this) { result ->
            when (result) {
                is NetworkResult.Error<*> -> {
                    dismissLoadingDialog()
                    showSnackbar("Paylaşım linki geçerli değil!")
                }
                is NetworkResult.Loading<*> -> {}
                is NetworkResult.Success<*> -> {
                    dismissLoadingDialog()
                    val bundleCode = result.data?.bundleCode
                    val bundleName = result.data?.bundleName
                    val bundleCodeName = "$bundleCode / $bundleName"
                    binding.bundleName.text = bundleCodeName
                }
            }
        }
    }
    private fun getCompanyData(shareToken: String) {

        Log.d(TAG, "Share token: $shareToken")

        viewModel.routeSharedCustomerData(shareToken)

        viewModel.postResult6.observe(this) { result ->
            when (result) {
                is NetworkResult.Error<*> -> { showSnackbar("Paylaşım linki geçerli değil!") }
                is NetworkResult.Loading<*> -> {}
                is NetworkResult.Success<*> -> {
                    val customerName = result.data?.customerName
                    binding.companyName.text = customerName
                }
            }
        }
    }

    // JWT TOKEN
    private fun getLinkDataJWT(jwt: String, urlLink: String){

        viewModel.routeJWTbundle(jwt)

        // Observers for Retrofit
        viewModel.postResult1.observe(this) { result ->

            when (result) {

                is NetworkResult.Loading<*> -> {
                    showLoadingDialog()
                }

                is NetworkResult.Success -> {
                    // success state
                    // println(result.data)
                    dismissLoadingDialog()
                    receivedJWTdata(result.data)
                    val editor = prefs.edit()
                    editor.putString("KAI_JWT_TOKEN", jwt)
                    editor.putString("KAI_URL_LINK", urlLink)
                    editor.putBoolean("KAI_IS_JWT", true)
                    editor.putBoolean("KAI_LINK_VAR", true)
                    editor.apply()
                    //
                    checkTheLastLink()
                }

                is NetworkResult.Error -> {
                    println(result)
                    dismissLoadingDialog()
                    showSnackbar("Paylaşım linki geçerli değil!")
                }
            }
        }
    }

    private fun receivedJWTdata (data: ResponseJwtBundle?) {

        binding.companyName.text = data?.customer_name
        val bundleName = "${data?.bundle_code} / ${data?.bundle_name}"
        binding.bundleName.text = bundleName
        linkVar = true

        bid = data?.bid
        cid = data?.auto_id

        visibilityPanelForKahraman(false)
        visibilityPanelForLinks(false)
        visibilityPanelForCompany(true)

        isScanning = false
        updateCameraUI(true)
        requestCameraAndShow()
    }

    private fun takePhoto() {

        aiTokenCount?.let {
            if (it < 1){
                showSnackbar("Belge yüklemek için AI Token satın almalısınız.")
                return
            }
        }

        val filename = SimpleDateFormat(PhotoManager.FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        val photoFile = File(
            outputDirectory,
            filename
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    //val msg = "Photo capture succeeded: $savedUri"
                    //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    handleImageCaptured(savedUri)
                }
            })
    }

    private fun continuePhotoCapture() {
        val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
        val receiptView = findViewById<ImageView>(R.id.receipt_view)

        receiptView.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        binding.cameraButton.isEnabled = true
    }

    private fun handleImageCaptured(imageUri: Uri){

        val cameraButton = binding.cameraButton
        cameraButton.isEnabled = false

        val viewFinder = findViewById<PreviewView>(R.id.viewFinder)
        val receiptView = findViewById<ImageView>(R.id.receipt_view)

        // Make the camera preview invisible
        viewFinder.visibility = View.GONE

        // Set the captured image URI to the ImageView and make it visible
        receiptView.setImageURI(imageUri)
        receiptView.visibility = View.VISIBLE

        // Process file
        val fileDetails = getFileDetailsFromUri( imageUri)
        val mimeType = fileDetails.mimeType
        val fileSizeInBytes = fileDetails.fileSize
        val uuid: UUID = UUID.randomUUID()

        if (fileSizeInBytes != null) {
            if(fileSizeInBytes > 1024 * 1024) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val compressedUri = compressImage(this@MainActivity, imageUri)
                    withContext(Dispatchers.Main) {
                        if (compressedUri != null) {
                            val fileDetails = getFileDetailsFromUri( imageUri)
                            val fileSizeInBytes = fileDetails.fileSize
                            val mimeType = fileDetails.mimeType
                            val postData = UploadRequest(cid, bid,0,uuid.toString(),"-",fileSizeInBytes,".jpg",0,mimeType.toString())
                            postActionForUploadLink(compressedUri, postData)
                            deleteFileFromUri(imageUri)
                        } else {
                            showSnackbar("Belge yüklenemedi!")
                        }
                    }
                }
            }
        } else {
            val postData = UploadRequest(cid, bid,0,uuid.toString(),"-",fileSizeInBytes,".jpg",0,mimeType.toString())
            postActionForUploadLink(imageUri, postData)
        }
    }

    private fun postActionForUploadLink(imageUri: Uri, postData: UploadRequest) {
        val isJWT = prefs.getBoolean("KAI_IS_JWT", true)
        if (isJWT) {
            postActionForUploadLinkJWT(imageUri, postData)
        } else {
            postActionForUploadLinkForShared(imageUri, postData)
        }
    }

    private fun postActionForUploadLinkForShared(imageUri: Uri, postData: UploadRequest) {
        val shareToken = prefs.getString("KAI_SHARE_TOKEN", "------")

        viewModel.routeSharedUploadGetPresigned(shareToken, postData)

        // Observers for Retrofit
        viewModel.postResult9.observe(this) { result ->

            when (result) {

                is NetworkResult.Loading<*> -> {
                    // Show some progress here
                }

                is NetworkResult.Success -> {
                    // success state
                    //println(result.data)
                    uploadFileNow(result.data, uri = imageUri, true)
                }

                is NetworkResult.Error -> {
                    println(result)
                    showSnackbar("Beklenmeyen bir hata oluştu!")
                }
            }
        }

    }

    private fun postActionForUploadLinkJWT(imageUri: Uri, postData: UploadRequest) {
        val JWT = prefs.getString("KAI_JWT_TOKEN", "------")

        viewModel.routeJWTupload2_presigned(JWT, postData)

        // Observers for Retrofit
        viewModel.postResult2.observe(this) { result ->

            when (result) {

                is NetworkResult.Loading<*> -> {
                    // Show some progress here
                }

                is NetworkResult.Success -> {
                    // success state
                    //println(result.data)
                    uploadFileNow(result.data, uri = imageUri, true)

                }

                is NetworkResult.Error -> {
                    println(result)
                    showSnackbar("Beklenmeyen bir hata oluştu!")
                }


            }

        }

    }

    private fun getUserCredits() {

        if (!linkVar) { return }

        val isJWT = prefs.getBoolean("KAI_IS_JWT", true)
        if (isJWT) {
            val jwt : String? = prefs.getString("KAI_JWT_TOKEN","---------")
            viewModel.routeJWTuserCredits(jwt)

            viewModel.postResult4.observe(this) { result ->

                when (result) {
                    is NetworkResult.Error<*> -> { showSnackbar("Beklenmeyen bir hata oluştu!")}
                    is NetworkResult.Loading<*> -> {}
                    is NetworkResult.Success<*> -> {
                        val credits = result.data?.credits
                        val aiCredits = "AI Token: $credits"
                        aiTokenCount = credits
                        binding.uploadStatusTextView.text = aiCredits
                    }
                }

            }
        } else {
            val shareToken:String? = prefs.getString("KAI_SHARE_TOKEN","---------")

            viewModel.routeSharedUserCredits(shareToken)

            viewModel.postResult8.observe(this) { result ->

                when (result) {
                    is NetworkResult.Error<*> -> { showSnackbar("Beklenmeyen bir hata oluştu!")}
                    is NetworkResult.Loading<*> -> {}
                    is NetworkResult.Success<*> -> {
                        val credits = result.data?.credits
                        val aiCredits = "AI Token: $credits"
                        aiTokenCount = credits
                        binding.uploadStatusTextView.text = aiCredits
                    }
                }

            }

        }


    }

    private fun uploadFileNow(presignedUrlResponse: PresignedUrlResponse?, uri: Uri, tekrar: Boolean = true) {

        val uploadFile: File? = getFileFromUri(this, uri)

        if (uploadFile != null && presignedUrlResponse != null) {
            viewModel.uploadFileS3(presignedUrlResponse, uploadFile)

            viewModel.postResult3.observe(this) { result ->

                when (result) {

                    is NetworkResult.Loading<*> -> {
                        continuePhotoCapture()
                    }

                    is NetworkResult.Success -> {
                        // success state
                        //println("Upload is successfull")
                        deleteFileFromUri(uri) // delete the image after successful upload
                    }

                    is NetworkResult.Error -> {
                        //println(result)
                        if (tekrar) {
                            uploadFileNow(presignedUrlResponse, uri, false)
                        } else {
                            showSnackbar("Belge yüklenemedi!")
                            deleteFileFromUri(uri) // Still delete the image even after failed upload
                        }

                    }
                }
            }
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
                    it.surfaceProvider = viewFinder.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                if (isScanning) {
                    binding.overlayView.startOverlay()
                    startBarcodeScanner(cameraProvider, cameraSelector, preview)
                } else {
                    binding.overlayView.stopOverlay()
                    startPhotoCapture(cameraProvider, cameraSelector, preview)
                }

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    /* Photo Taking Functions */
    private fun startPhotoCapture(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector, preview: Preview) {
        imageCapture = ImageCapture.Builder()
            .build()

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }

    /* Scanning Functions */
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

    private fun showLoadingDialog(){
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.isCancelable = false
        loadingDialog.show(supportFragmentManager, LOADING_DIALOG_TAG)
    }

    private fun dismissLoadingDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag(LOADING_DIALOG_TAG) as? LoadingDialogFragment
        if (dialogFragment != null && dialogFragment.isAdded) {
            dialogFragment.dismiss()
        }
    }

    private fun handleQRcodeScanned(qrValue: String){
        Log.d(TAG, qrValue)
        stopScanning()
        binding.textInputShareLink.setText(qrValue)
        checkShareLink(qrValue)
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

        updateCameraUI(false)
        visibilityPanelForKahraman(true)
        visibilityPanelForLinks(true)
    }

    private fun stopCamera() {
        // Call the comprehensive release function
        releaseCamera()

        updateCameraUI(false)
        visibilityPanelForCompany(false)
        visibilityPanelForKahraman(true)
        visibilityPanelForLinks(true)

    }

    private fun updateCameraUI(goster: Boolean){
        val visibilityStatus = if(goster) View.VISIBLE else View.GONE
        val receiptView = findViewById<ImageView>(R.id.receipt_view)
        binding.cameraContainer.visibility = visibilityStatus
        if(isScanning){
            receiptView.visibility = View.GONE
            binding.cancelQrScan.visibility = visibilityStatus
            binding.scanViewText.visibility = visibilityStatus
            if (goster) {
                binding.scanViewText.visibility = View.VISIBLE
            }  else {
                binding.scanViewText.visibility = View.GONE
            }
        } else {
            binding.cancelQrScan.visibility = View.GONE
            binding.scanViewText.visibility = View.GONE
            if (goster) {
                binding.scanViewText.visibility = View.GONE
            }
        }
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
            binding.panelForLinkActions.visibility = View.VISIBLE
        } else {
            // The input is not a valid URL, hide the button
            binding.btnUseShareLink.visibility = View.GONE
            binding.panelForLinkActions.visibility = View.GONE
        }
    }

    // Panels
    private fun visibilityPanelForKahraman(goster: Boolean) {
        binding.panelForKahraman.visibility = if (goster) View.VISIBLE else View.GONE
    }

    private fun visibilityPanelForLinks(goster: Boolean) {
        binding.panelForLinks.visibility = if (goster) View.VISIBLE else View.GONE
    }

    private fun visibilityPanelForCompany(goster: Boolean) {
        binding.panelForCompany.visibility = if (goster) View.VISIBLE else View.GONE
        binding.panelForButtons.visibility = if (goster) View.VISIBLE else View.GONE
        if ( goster ) {
            getUserCredits()
            updateCameraContainerConstraints(R.id.panelForCompany, R.id.panelForButtons)
        }
    }

    private fun checkTheLastLink () {
        val linkVar = prefs.getBoolean("KAI_LINK_VAR", false)
        if (linkVar) {
            binding.btnUseLastLink.visibility = View.VISIBLE
        } else {
            binding.btnUseLastLink.visibility = View.GONE
        }
    }

    fun Activity.showSnackbar(message: String) {
        // A Snackbar needs a 'View' to attach to.
        // We can find the root view of the activity.
        val rootView = findViewById<View>(android.R.id.content)

        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun updateCameraContainerConstraints(topAnchorId: Int, bottomAnchorId: Int) {
        val constraintSet = ConstraintSet()
        // Clone the existing constraints from the root layout
        constraintSet.clone(binding.main)

        // Clear the existing top constraint of the camera container
        constraintSet.clear(R.id.camera_container, ConstraintSet.TOP)
        constraintSet.clear(R.id.camera_container, ConstraintSet.BOTTOM)


        // Set the new top constraint to connect to the bottom of the new anchor view
        constraintSet.connect(
            R.id.camera_container,
            ConstraintSet.TOP,
            topAnchorId,
            ConstraintSet.BOTTOM
        )

        // Set the new BOTTOM constraint
        constraintSet.connect(
            R.id.camera_container,
            ConstraintSet.BOTTOM,
            bottomAnchorId,
            ConstraintSet.TOP
        )

        // Apply the modified constraints back to the layout
        constraintSet.applyTo(binding.main)
    }


}
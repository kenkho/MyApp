package com.kenkho.nutritracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kenkho.nutritracker.fmt
import com.kenkho.nutritracker.ocr.LabelTextReader
import com.kenkho.nutritracker.ocr.ParsedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onBack: () -> Unit, onResult: (ParsedLabel) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(recognizer) { onDispose { recognizer.close() } }

    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ParsedLabel?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun recognize(image: InputImage, onFinished: () -> Unit = {}) {
        busy = true
        error = null
        recognizer.process(image)
            .addOnSuccessListener { text ->
                val parsed = LabelTextReader.parse(text)
                if (parsed.foundAnything) result = parsed
                else error = "Couldn't read a Nutrition Facts label. Try again with the label flat, well lit and filling the frame."
            }
            .addOnFailureListener { error = "Text recognition failed: ${it.localizedMessage}" }
            .addOnCompleteListener {
                busy = false
                onFinished()
            }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            runCatching { InputImage.fromFilePath(context, uri) }
                .onSuccess { recognize(it) }
                .onFailure { error = "Couldn't open that image." }
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan label") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (hasPermission) {
                    CameraPreview(imageCapture, Modifier.fillMaxSize())
                } else {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Camera permission is needed to scan labels. You can also pick a photo instead.")
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow camera")
                        }
                    }
                }
                if (busy) CircularProgressIndicator()
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                result?.let { ScanResultCard(it) }

                if (result != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { result = null }, Modifier.weight(1f)) { Text("Retake") }
                        Button(onClick = { onResult(result!!) }, Modifier.weight(1f)) { Text("Use values") }
                    }
                } else {
                    Text(
                        "Point the camera at the Nutrition Facts panel so it fills the frame, then tap Capture.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) { Text("From photo") }
                        Button(
                            onClick = {
                                busy = true
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val input = runCatching {
                                                InputImage.fromBitmap(image.toBitmap(), image.imageInfo.rotationDegrees)
                                            }
                                            image.close()
                                            input.onSuccess { recognize(it) }
                                                .onFailure {
                                                    busy = false
                                                    error = "Couldn't process the photo."
                                                }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            busy = false
                                            error = "Capture failed: ${exception.localizedMessage}"
                                        }
                                    },
                                )
                            },
                            enabled = hasPermission && !busy,
                            modifier = Modifier.weight(1f),
                        ) { Text("Capture") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            provider = providerFuture.get().also {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                it.unbindAll()
                it.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun ScanResultCard(p: ParsedLabel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Found on label (per serving)", style = MaterialTheme.typography.titleSmall)
            Text("Serving size: ${p.servingSize ?: "—"}")
            Text("Calories: ${p.calories?.fmt() ?: "—"}")
            Text("Carbohydrate: ${p.carbsG?.let { "${it.fmt()} g" } ?: "—"}")
            Text("Sugars: ${p.sugarG?.let { "${it.fmt()} g" } ?: "—"}")
            Text("Sodium: ${p.sodiumMg?.let { "${it.fmt()} mg" } ?: "—"}")
            Text(
                "You can correct any value on the next screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.foleyit.itflow.ui.screens.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.navigation.Screen
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBarcodeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanning by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Point camera at barcode or serial number") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onBarcodeFound(raw: String) {
        if (!scanning) return
        scanning = false
        status = "Found: $raw — searching…"
        scope.launch {
            val result = runCatching { ApiClient.service().getAssets(search = raw) }.getOrNull()
            val first = result?.data?.firstOrNull()
            if (first != null) {
                navController.navigate(Screen.AssetDetail.go(first.id)) {
                    popUpTo(Screen.ScanBarcode.route) { inclusive = true }
                }
            } else {
                status = "No asset found for \"$raw\". Tap back to try again."
                scanning = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !hasCameraPermission -> {
                    // Permission denied — show explanation
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📷", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Camera access is needed to scan asset barcodes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
                else -> {
                    // Camera preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                val provider = future.get()
                                val preview = Preview.Builder().build()
                                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysis.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    ZxingAnalyzer { raw -> if (scanning) onBarcodeFound(raw) }
                                )
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, analysis
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Viewfinder box
                    Box(
                        Modifier.align(Alignment.Center).size(240.dp)
                            .border(2.dp, Color.White, MaterialTheme.shapes.medium)
                    )
                    // Status label
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            status,
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private class ZxingAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    // TRY_HARDER trades a little decode latency for a real tolerance boost on skewed/lower
    // quality captures — worthwhile here since we no longer bail after the first miss per frame.
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.TRY_HARDER to true))
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val plane = imageProxy.planes.firstOrNull()
        if (plane != null) {
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
            // CameraX delivers the Y-plane in sensor orientation; rotationDegrees is how much it
            // must be rotated clockwise to appear upright. QR decodes fine without this because
            // its finder patterns are orientation-invariant, but 1D linear barcodes (UPC/EAN/
            // Code128/Codabar/ITF — what asset serial labels actually use) are direction-sensitive
            // and fail outright when scanned sideways. This was the actual "only QR scans" bug.
            val (rotated, w, h) = rotateYuvPlane(
                data, imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees
            )
            val source = PlanarYUVLuminanceSource(rotated, w, h, 0, 0, w, h, false)
            runCatching { onResult(reader.decode(BinaryBitmap(HybridBinarizer(source))).text) }
        }
        imageProxy.close()
    }
}

/**
 * Rotates a single-byte-per-pixel plane (here, a YUV luma/Y plane) clockwise by
 * [rotationDegrees], which must be one of 0/90/180/270 (the only values CameraX's
 * `ImageInfo.rotationDegrees` ever reports). Returns the rotated buffer plus its new
 * (width, height) — for 90/270 these are swapped versus the input.
 *
 * Assumes the plane is tightly packed (row stride == width), matching the assumption the
 * pre-existing code already made by feeding the raw buffer straight into
 * [PlanarYUVLuminanceSource] with `width` doubling as that source's row-stride parameter.
 */
private fun rotateYuvPlane(data: ByteArray, width: Int, height: Int, rotationDegrees: Int): Triple<ByteArray, Int, Int> {
    if (rotationDegrees == 0) return Triple(data, width, height)
    val out = ByteArray(data.size)
    return when (rotationDegrees) {
        90 -> {
            for (y in 0 until height) for (x in 0 until width) {
                out[x * height + (height - 1 - y)] = data[y * width + x]
            }
            Triple(out, height, width)
        }
        180 -> {
            val last = data.size - 1
            for (i in data.indices) out[last - i] = data[i]
            Triple(out, width, height)
        }
        270 -> {
            for (y in 0 until height) for (x in 0 until width) {
                out[(width - 1 - x) * height + y] = data[y * width + x]
            }
            Triple(out, height, width)
        }
        else -> Triple(data, width, height) // defensive fallback; CameraX never reports other values
    }
}

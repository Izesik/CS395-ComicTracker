package com.moravian.comictracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.moravian.comictracker.AppLog
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun BarcodeCameraView(onBarcodeDetected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreviewWithScanner(
                onBarcodeDetected = onBarcodeDetected,
                lifecycleOwner = lifecycleOwner
            )

            // Scan-target frame
            Box(
                modifier = Modifier
                    .size(320.dp, 160.dp)
                    .align(Alignment.Center)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            )
            Text(
                text = "Align both barcodes in frame",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 200.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera access is required to scan barcodes",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }

        // Back button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 12.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CameraPreviewWithScanner(
    onBarcodeDetected: (String) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanned = remember { AtomicBoolean(false) }
    val accumulator = remember { BarcodeScanAccumulator() }
    val zxingReader = remember { createZxingReader() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                .build()
        )
    }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            textRecognizer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            processImageProxy(
                                imageProxy = imageProxy,
                                scanner = scanner,
                                textRecognizer = textRecognizer,
                                zxingReader = zxingReader,
                                accumulator = accumulator,
                                scanned = scanned,
                                onBarcodeDetected = onBarcodeDetected
                            )
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    textRecognizer: TextRecognizer,
    zxingReader: MultiFormatReader,
    accumulator: BarcodeScanAccumulator,
    scanned: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || scanned.get()) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    decodeComicBarcodeWithZxing(imageProxy, zxingReader)?.let { raw ->
        if (scanned.compareAndSet(false, true)) {
            AppLog.d("ComicTrackerBarcode", "ZXing detected comic barcode raw=$raw")
            onBarcodeDetected(raw)
        }
        imageProxy.close()
        return
    }

    val barcodeTask = scanner.process(inputImage)
    val textTask = textRecognizer.process(inputImage)

    Tasks.whenAllComplete(barcodeTask, textTask).addOnCompleteListener {
        try {
            val barcodes = if (barcodeTask.isSuccessful) barcodeTask.result ?: emptyList() else emptyList()
            val visionText: Text? = if (textTask.isSuccessful) textTask.result else null

            if (barcodes.isNotEmpty()) {
                AppLog.d(
                    "ComicTrackerBarcode",
                    "ML Kit detected barcodes=${barcodes.map { "format=${it.format}, raw=${it.rawValue}" }}"
                )
            }

            val upcRaw = barcodes.firstOrNull()?.rawValue?.filter { it.isDigit() }
            val combined = if (upcRaw?.length == 12 && visionText != null) {
                findSupplementInText(visionText, upcRaw)?.let { supplement ->
                    AppLog.d("ComicTrackerBarcode", "OCR supplement=$supplement combined with UPC=$upcRaw")
                    upcRaw + supplement
                } ?: upcRaw
            } else {
                upcRaw
            }

            combined?.let { raw ->
                emitIfReady(raw, accumulator, scanned, onBarcodeDetected)
            } ?: accumulator.readyPending()?.let { raw ->
                emitBarcode(raw, "Pending UPC delay elapsed", scanned, onBarcodeDetected)
            }
        } finally {
            imageProxy.close()
        }
    }
}

private fun createZxingReader(): MultiFormatReader =
    MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.UPC_E
                ),
                DecodeHintType.ALLOWED_EAN_EXTENSIONS to intArrayOf(5),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

private fun emitIfReady(
    raw: String,
    accumulator: BarcodeScanAccumulator,
    scanned: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit
) {
    val digits = raw.filter { it.isDigit() }
    val ready = accumulator.accept(digits)
    if (ready != null) {
        emitBarcode(ready, "Barcode ready", scanned, onBarcodeDetected)
    }
}

private fun emitBarcode(
    raw: String,
    reason: String,
    scanned: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit
) {
    if (scanned.compareAndSet(false, true)) {
        AppLog.d("ComicTrackerBarcode", "$reason raw=$raw")
        onBarcodeDetected(raw)
    }
}

private class BarcodeScanAccumulator {
    private var pendingUpc: String? = null
    private var pendingSinceMillis: Long = 0L

    @Synchronized
    fun accept(raw: String): String? {
        if (raw.isBlank()) return null
        if (raw.length > UPC_A_LENGTH) {
            clear()
            return raw
        }
        if (raw.length != UPC_A_LENGTH) return null

        val now = System.currentTimeMillis()
        if (pendingUpc != raw) {
            pendingUpc = raw
            pendingSinceMillis = now
            AppLog.d(
                "ComicTrackerBarcode",
                "Holding UPC-A raw=$raw while looking for 5-digit supplemental"
            )
            return null
        }

        return if (now - pendingSinceMillis >= SUPPLEMENTAL_WAIT_MILLIS) {
            AppLog.d("ComicTrackerBarcode", "No supplemental found for UPC-A raw=$raw")
            clear()
            raw
        } else {
            null
        }
    }

    @Synchronized
    fun readyPending(): String? {
        val raw = pendingUpc ?: return null
        return if (System.currentTimeMillis() - pendingSinceMillis >= SUPPLEMENTAL_WAIT_MILLIS) {
            clear()
            raw
        } else {
            null
        }
    }

    private fun clear() {
        pendingUpc = null
        pendingSinceMillis = 0L
    }

    private companion object {
        const val UPC_A_LENGTH = 12
        const val SUPPLEMENTAL_WAIT_MILLIS = 3000L
    }
}

private fun decodeComicBarcodeWithZxing(
    imageProxy: ImageProxy,
    reader: MultiFormatReader
): String? {
    val yPlane = imageProxy.planes.firstOrNull() ?: return null
    val buffer = yPlane.buffer
    val yData = ByteArray(buffer.remaining())
    buffer.get(yData)

    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = yPlane.rowStride
    val source = PlanarYUVLuminanceSource(
        yData,
        rowStride,
        height,
        0,
        0,
        width,
        height,
        false
    )

    return try {
        val result = decodeFromPossibleOrientations(source, reader) ?: return null
        val main = result.text.filter { it.isDigit() }
        val extension = (result.resultMetadata[ResultMetadataType.UPC_EAN_EXTENSION] as? String)
            ?.filter { it.isDigit() }
            .orEmpty()
        val combined = if (extension.isNotEmpty() && !main.endsWith(extension)) {
            main + extension
        } else {
            main
        }

        AppLog.d(
            "ComicTrackerBarcode",
            "ZXing decoded format=${result.barcodeFormat}, main=$main, extension=$extension, combined=$combined"
        )
        combined.takeIf { it.length > main.length }
    } catch (_: NotFoundException) {
        null
    } catch (exception: Exception) {
        AppLog.e("ComicTrackerBarcode", "ZXing barcode decode failed", exception)
        null
    } finally {
        reader.reset()
        buffer.rewind()
    }
}

private fun findSupplementInText(text: Text, mainUpc: String): String? {
    for (block in text.textBlocks) {
        for (line in block.lines) {
            val digits = line.text
                .replace('O', '0').replace('o', '0')
                .replace('I', '1').replace('l', '1')
                .filter { it.isDigit() }
            if (digits.length == 5 && !mainUpc.contains(digits)) {
                return digits
            }
        }
    }
    return null
}

private fun decodeFromPossibleOrientations(
    source: LuminanceSource,
    reader: MultiFormatReader
) = buildList {
    add(source)
    if (source.isRotateSupported) {
        val rotated = source.rotateCounterClockwise()
        add(rotated)
        add(rotated.rotateCounterClockwise())
        add(rotated.rotateCounterClockwise().rotateCounterClockwise())
    }
}.firstNotNullOfOrNull { candidate ->
    try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(candidate)))
    } catch (_: NotFoundException) {
        null
    } finally {
        reader.reset()
    }
}

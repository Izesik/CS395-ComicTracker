package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun BarcodeCameraView(onBarcodeDetected: (String) -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val scannerView = remember {
            BarcodeScannerUIView(onBarcodeDetected = onBarcodeDetected)
        }
        DisposableEffect(scannerView) {
            scannerView.start()
            onDispose { scannerView.stop() }
        }
        UIKitView(
            factory = { scannerView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .size(260.dp, 160.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
        )
        Text(
            text = "Point camera at barcode",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 200.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

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

@OptIn(ExperimentalForeignApi::class)
private class BarcodeScannerUIView(
    private val onBarcodeDetected: (String) -> Unit
) : UIView(frame = CGRectZero.readValue()), AVCaptureMetadataOutputObjectsDelegateProtocol {
    private val session = AVCaptureSession()
    private val metadataOutput = AVCaptureMetadataOutput()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private var didScan = false
    private var configured = false

    init {
        backgroundColor = UIColor.blackColor
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
        configureSession()
    }

    fun start() {
        if (configured && !session.running) {
            session.startRunning()
        }
    }

    fun stop() {
        if (session.running) {
            session.stopRunning()
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }

    private fun configureSession() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) ?: return

        if (session.canAddInput(input)) {
            session.addInput(input)
        }
        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)
        }

        metadataOutput.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
        metadataOutput.metadataObjectTypes = listOf(
            AVMetadataObjectTypeEAN13Code,
            AVMetadataObjectTypeEAN8Code,
            AVMetadataObjectTypeUPCECode
        )
        configured = true
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (didScan) return
        val rawValue = didOutputMetadataObjects
            .firstNotNullOfOrNull { (it as? AVMetadataMachineReadableCodeObject)?.stringValue }
            ?: return

        didScan = true
        stop()
        onBarcodeDetected(rawValue)
    }
}

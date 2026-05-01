package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moravian.comictracker.ui.viewmodels.BarcodeScanState
import com.moravian.comictracker.ui.viewmodels.BarcodeScanViewModel

// Platform-specific camera preview — actuals in androidMain and iosMain.
@Composable
expect fun BarcodeCameraView(onBarcodeDetected: (String) -> Unit, onDismiss: () -> Unit)

@Composable
fun BarcodeScanRoute(
    onIssueFound: (issueId: Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: BarcodeScanViewModel = viewModel()
) {
    val scanState = viewModel.state
    var scanSession by remember { mutableIntStateOf(0) }

    LaunchedEffect(scanState) {
        if (scanState is BarcodeScanState.Found) {
            onIssueFound(scanState.issueId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(scanSession) {
            BarcodeCameraView(
                onBarcodeDetected = { viewModel.onBarcodeScanned(it) },
                onDismiss = onDismiss
            )
        }

        when (scanState) {
            is BarcodeScanState.Loading -> LoadingOverlay(upc = scanState.upc)
            is BarcodeScanState.NotFound -> NotFoundOverlay(
                title = "Comic not found",
                message = "This barcode wasn't found.",
                onRetry = {
                    scanSession += 1
                    viewModel.reset()
                },
                onDismiss = onDismiss
            )
            is BarcodeScanState.Error -> NotFoundOverlay(
                title = "Lookup failed",
                message = "Check your connection and Metron credentials, then try again.",
                onRetry = {
                    scanSession += 1
                    viewModel.reset()
                },
                onDismiss = onDismiss
            )
            else -> {}
        }
    }
}

@Composable
private fun LoadingOverlay(upc: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Looking up $upc…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NotFoundOverlay(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0xFFAAAAAA),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Scan Again") }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFAAAAAA))
            }
        }
    }
}

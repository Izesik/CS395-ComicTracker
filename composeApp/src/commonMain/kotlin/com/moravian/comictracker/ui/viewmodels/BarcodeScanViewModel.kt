package com.moravian.comictracker.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.AppLog
import com.moravian.comictracker.network.MetronApi
import kotlinx.coroutines.launch

sealed class BarcodeScanState {
    data object Scanning : BarcodeScanState()
    data class Loading(val upc: String) : BarcodeScanState()
    data class Found(val issueId: Int) : BarcodeScanState()
    data object NotFound : BarcodeScanState()
    data object SupplementMissing : BarcodeScanState()
    data object Error : BarcodeScanState()
}

class BarcodeScanViewModel : ViewModel() {
    private val metron = MetronApi()

    var state by mutableStateOf<BarcodeScanState>(BarcodeScanState.Scanning)
        private set

    fun onBarcodeScanned(upc: String) {
        if (state !is BarcodeScanState.Scanning) return
        AppLog.d(TAG, "Barcode scanned raw=$upc")
        val normalizedUpc = upc.filter { it.isDigit() }
        if (normalizedUpc.isBlank()) {
            AppLog.d(TAG, "Barcode ignored because it did not contain digits")
            state = BarcodeScanState.NotFound
            return
        }
        AppLog.d(TAG, "Barcode normalized upc=$normalizedUpc")
        if (normalizedUpc.length == UPC_A_ONLY_LENGTH) {
            AppLog.d(TAG, "Barcode has no supplement — Metron requires 17 digits")
            state = BarcodeScanState.SupplementMissing
            return
        }
        state = BarcodeScanState.Loading(normalizedUpc)
        viewModelScope.launch {
            state = try {
                val summary = metron.searchByUpc(normalizedUpc).results.firstOrNull()
                if (summary == null) {
                    BarcodeScanState.NotFound
                } else {
                    val cvId = summary.cvId ?: metron.getIssue(summary.id).cvId
                    AppLog.d(TAG, "Barcode lookup resolved cvIssueId=$cvId")
                    if (cvId != null) BarcodeScanState.Found(cvId) else BarcodeScanState.NotFound
                }
            } catch (exception: Exception) {
                AppLog.e(TAG, "Barcode lookup failed for upc=$normalizedUpc", exception)
                BarcodeScanState.Error
            }
        }
    }

    fun reset() {
        state = BarcodeScanState.Scanning
    }

    private companion object {
        const val TAG = "ComicTrackerBarcode"
        const val UPC_A_ONLY_LENGTH = 12
    }
}

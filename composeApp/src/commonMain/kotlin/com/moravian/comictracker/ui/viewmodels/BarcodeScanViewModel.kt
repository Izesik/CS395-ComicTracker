package com.moravian.comictracker.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.AppLog
import com.moravian.comictracker.network.MetronApi
import kotlinx.coroutines.launch

/** Possible states for the barcode scan flow. */
sealed class BarcodeScanState {
    /** Waiting for a barcode to be detected by the camera. */
    data object Scanning : BarcodeScanState()
    /** A barcode was detected and the Metron API is being queried for [upc]. */
    data class Loading(val upc: String) : BarcodeScanState()
    /** An issue was found; [issueId] is the ComicVine issue ID to navigate to. */
    data class Found(val issueId: Int) : BarcodeScanState()
    /** The barcode was not found in the Metron database. */
    data object NotFound : BarcodeScanState()
    /** Only the main UPC-A barcode was visible; the 5-digit supplement was missing. */
    data object SupplementMissing : BarcodeScanState()
    /** A network or API error occurred during lookup. */
    data object Error : BarcodeScanState()
}

/**
 * ViewModel for the Barcode Scan screen.
 *
 * Receives raw barcode strings from the camera layer, normalises them, validates
 * that a 5-digit supplement is present (required for Metron's 17-digit UPC format),
 * and performs a Metron API lookup to resolve a ComicVine issue ID.
 */
class BarcodeScanViewModel : ViewModel() {
    private val metron = MetronApi()

    /** Current scan state, observed directly by the composable. */
    var state by mutableStateOf<BarcodeScanState>(BarcodeScanState.Scanning)
        private set

    /**
     * Called by the camera layer when a barcode string is decoded.
     *
     * Normalises [upc] to digits only, validates its length, then triggers an API lookup.
     * Ignored if the ViewModel is not in the [BarcodeScanState.Scanning] state.
     */
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

    /** Resets the scan state to [BarcodeScanState.Scanning] so a new scan can begin. */
    fun reset() {
        state = BarcodeScanState.Scanning
    }

    private companion object {
        const val TAG = "ComicTrackerBarcode"
        /** A raw UPC-A barcode without its 5-digit supplement is too short for Metron. */
        const val UPC_A_ONLY_LENGTH = 12
    }
}

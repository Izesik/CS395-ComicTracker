package com.moravian.comictracker.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.network.MetronApi
import kotlinx.coroutines.launch

sealed class BarcodeScanState {
    data object Scanning : BarcodeScanState()
    data class Loading(val upc: String) : BarcodeScanState()
    data class Found(val issueId: Int) : BarcodeScanState()
    data object NotFound : BarcodeScanState()
    data object Error : BarcodeScanState()
}

class BarcodeScanViewModel : ViewModel() {
    private val metron = MetronApi()

    var state by mutableStateOf<BarcodeScanState>(BarcodeScanState.Scanning)
        private set

    fun onBarcodeScanned(upc: String) {
        if (state !is BarcodeScanState.Scanning) return
        val normalizedUpc = upc.filter { it.isDigit() }
        if (normalizedUpc.isBlank()) {
            state = BarcodeScanState.NotFound
            return
        }
        state = BarcodeScanState.Loading(normalizedUpc)
        viewModelScope.launch {
            state = try {
                val issueId = metron.searchByUpc(normalizedUpc).results.firstOrNull()?.cvId
                if (issueId != null) BarcodeScanState.Found(issueId) else BarcodeScanState.NotFound
            } catch (_: Exception) {
                BarcodeScanState.Error
            }
        }
    }

    fun reset() {
        state = BarcodeScanState.Scanning
    }
}

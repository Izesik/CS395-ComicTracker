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
}

class BarcodeScanViewModel : ViewModel() {
    private val metron = MetronApi()

    var state by mutableStateOf<BarcodeScanState>(BarcodeScanState.Scanning)
        private set

    fun onBarcodeScanned(upc: String) {
        if (state !is BarcodeScanState.Scanning) return
        state = BarcodeScanState.Loading(upc)
        viewModelScope.launch {
            val issueId = metron.searchByUpc(upc).results.firstOrNull()?.cvId
            state = if (issueId != null) BarcodeScanState.Found(issueId) else BarcodeScanState.NotFound
        }
    }

    fun reset() {
        state = BarcodeScanState.Scanning
    }
}

package com.moravian.comictracker.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.network.ComicRepository
import kotlinx.coroutines.launch

sealed class BarcodeScanState {
    data object Scanning : BarcodeScanState()
    data class Loading(val upc: String) : BarcodeScanState()
    data class Found(val cvIssueId: Int) : BarcodeScanState()
    data object NotFound : BarcodeScanState()
}

class BarcodeScanViewModel : ViewModel() {
    private val repo = ComicRepository()

    var state by mutableStateOf<BarcodeScanState>(BarcodeScanState.Scanning)
        private set

    fun onBarcodeScanned(upc: String) {
        if (state !is BarcodeScanState.Scanning) return
        state = BarcodeScanState.Loading(upc)
        viewModelScope.launch {
            val cvId = repo.lookupByUpc(upc)
            state = if (cvId != null) BarcodeScanState.Found(cvId) else BarcodeScanState.NotFound
        }
    }

    fun reset() {
        state = BarcodeScanState.Scanning
    }
}

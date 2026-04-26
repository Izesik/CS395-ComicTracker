package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val volumes: List<ComicVineVolume>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadLatestMarvelVolumes()
    }

    fun refresh() {
        loadLatestMarvelVolumes()
    }

    private fun loadLatestMarvelVolumes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val response = api.getLatestMarvelVolumes()
                println("ComicVine response: status=${response.statusCode}, total=${response.totalResults}, page=${response.pageResults}")
                _uiState.value = HomeUiState.Success(response.results)
            } catch (e: Exception) {
                println("ComicVine error: ${e::class.simpleName}: ${e.message}")
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load comics")
            }
        }
    }
}

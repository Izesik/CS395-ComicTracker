package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

sealed class ComicDetailUiState {
    data object Loading : ComicDetailUiState()
    data class Success(val volume: ComicVineVolume) : ComicDetailUiState()
    data class Error(val message: String) : ComicDetailUiState()
}

class ComicDetailViewModel(private val volumeId: Int) : ViewModel() {
    private val api = ComicVineApi()
    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    init {
        loadVolumeDetails()
    }

    private fun loadVolumeDetails() {
        viewModelScope.launch {
            _uiState.value = ComicDetailUiState.Loading
            try {
                val response = api.getVolume(volumeId)
                _uiState.value = ComicDetailUiState.Success(response.results)
            } catch (e: Exception) {
                _uiState.value = ComicDetailUiState.Error(e.message ?: "Failed to load comic details")
            }
        }
    }

    companion object {
        fun factory(volumeId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                ComicDetailViewModel(volumeId) as T
        }
    }
}

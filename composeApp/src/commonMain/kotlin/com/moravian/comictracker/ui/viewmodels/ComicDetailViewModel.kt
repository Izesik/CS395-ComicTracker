package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssue
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

class ComicDetailViewModel(
    private val volumeId: Int,
    private val dao: ComicDao
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)
    val addState: StateFlow<AddCollectionState> = _addState.asStateFlow()

    private val _issues = MutableStateFlow<List<ComicVineIssue>>(emptyList())
    val issues: StateFlow<List<ComicVineIssue>> = _issues.asStateFlow()

    init {
        loadVolumeDetails()
        loadIssues()
        checkIfInCollection()
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

    private fun loadIssues() {
        viewModelScope.launch {
            try {
                val response = api.getIssuesByVolume(volumeId)
                _issues.value = response.results.sortedBy { it.issueNumber.toDoubleOrNull() ?: 0.0 }
            } catch (_: Exception) {
                // Issues grid is optional — failure is silent
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getSeriesByComicvineId(volumeId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
        }
    }

    fun addToCollection() {
        val volume = (uiState.value as? ComicDetailUiState.Success)?.volume ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding
            dao.insertSeries(
                SeriesEntity(
                    comicvineId = volume.id,
                    title = volume.name,
                    publisher = volume.publisher?.name,
                    coverImageUrl = volume.image?.mediumUrl
                )
            )
            _addState.value = AddCollectionState.Added
        }
    }

    companion object {
        fun factory(volumeId: Int, database: ComicTrackerDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    ComicDetailViewModel(volumeId, database.comicDao()) as T
            }
    }
}

package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssueSummary
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.network.toUserFacingNetworkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

sealed class ComicDetailUiState {
    data object Loading : ComicDetailUiState()
    data class Success(val series: ComicVineVolume) : ComicDetailUiState()
    data class Error(val message: String) : ComicDetailUiState()
}

class ComicDetailViewModel(
    private val seriesId: Int,
    private val dao: ComicDao
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)
    val addState: StateFlow<AddCollectionState> = _addState.asStateFlow()

    private val _issues = MutableStateFlow<List<ComicVineIssueSummary>>(emptyList())
    val issues: StateFlow<List<ComicVineIssueSummary>> = _issues.asStateFlow()

    init {
        loadSeriesDetails()
        loadIssues()
        checkIfInCollection()
    }

    private fun loadSeriesDetails() {
        viewModelScope.launch {
            _uiState.value = ComicDetailUiState.Loading
            try {
                val series = api.getVolume(seriesId)
                _uiState.value = ComicDetailUiState.Success(series)
            } catch (e: Exception) {
                _uiState.value = ComicDetailUiState.Error(
                    e.toUserFacingNetworkMessage("ComicVine", "Failed to load series details")
                )
            }
        }
    }

    private fun loadIssues() {
        viewModelScope.launch {
            try {
                _issues.value = api.getIssuesByVolume(seriesId).sortedBy { it.issueNumber.toDoubleOrNull() ?: 0.0 }
            } catch (_: Exception) {
                // Issues list is optional — failure is silent
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getSeriesByComicVineId(seriesId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
        }
    }

    fun addToCollection() {
        val series = (uiState.value as? ComicDetailUiState.Success)?.series ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding
            dao.insertSeries(
                SeriesEntity(
                    comicvineId = series.id,
                    title = series.name,
                    publisher = series.publisher?.name,
                    coverImageUrl = series.image?.coverUrl()
                )
            )
            _addState.value = AddCollectionState.Added
        }
    }

    fun removeFromCollection() {
        viewModelScope.launch {
            _addState.value = AddCollectionState.Removing
            val existing = dao.getSeriesByComicVineId(seriesId)
            if (existing != null) dao.deleteSeries(existing.id)
            _addState.value = AddCollectionState.Idle
        }
    }

    companion object {
        fun factory(seriesId: Int, database: ComicTrackerDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    ComicDetailViewModel(seriesId, database.comicDao()) as T
            }
    }
}

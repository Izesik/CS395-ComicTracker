package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.network.MetronApi
import com.moravian.comictracker.network.MetronIssueSummary
import com.moravian.comictracker.network.MetronSeriesSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeTab { Series, Issues }

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class SeriesSuccess(val series: List<MetronSeriesSummary>) : HomeUiState()
    data class IssuesSuccess(val issues: List<MetronIssueSummary>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {
    private val metron = MetronApi()

    private val _selectedTab = MutableStateFlow(HomeTab.Series)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedSeries: List<MetronSeriesSummary>? = null
    private var cachedIssues: List<MetronIssueSummary>? = null

    init {
        loadSeries()
    }

    fun selectTab(tab: HomeTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        when (tab) {
            HomeTab.Series -> cachedSeries?.let { _uiState.value = HomeUiState.SeriesSuccess(it) } ?: loadSeries()
            HomeTab.Issues -> cachedIssues?.let { _uiState.value = HomeUiState.IssuesSuccess(it) } ?: loadIssues()
        }
    }

    private fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val series = metron.getPopularSeries().results
                cachedSeries = series
                _uiState.value = HomeUiState.SeriesSuccess(series)
                loadSeriesCovers(series)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load series")
            }
        }
    }

    private suspend fun loadSeriesCovers(series: List<MetronSeriesSummary>) {
        var enrichedSeries = series
        series.take(MAX_SERIES_COVERS).forEach { item ->
            if (!item.image.isNullOrBlank()) return@forEach

            val cover = try {
                metron.getFirstIssueCoverForSeries(item.id)
            } catch (_: Exception) {
                null
            }

            if (!cover.isNullOrBlank()) {
                enrichedSeries = enrichedSeries.map { seriesItem ->
                    if (seriesItem.id == item.id) seriesItem.copy(image = cover) else seriesItem
                }
                cachedSeries = enrichedSeries
                if (_selectedTab.value == HomeTab.Series) {
                    _uiState.value = HomeUiState.SeriesSuccess(enrichedSeries)
                }
            }
        }
    }

    private fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val issues = metron.getRecentIssues().results.filter { it.series?.name?.isNotBlank() == true }
                cachedIssues = issues
                _uiState.value = HomeUiState.IssuesSuccess(issues)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load issues")
            }
        }
    }

    private companion object {
        const val MAX_SERIES_COVERS = 18
    }
}

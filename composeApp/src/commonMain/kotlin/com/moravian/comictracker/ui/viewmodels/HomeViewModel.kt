package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssueSummary
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.toUserFacingNetworkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class HomeTab { Series, Issues }

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class SeriesSuccess(val series: List<ComicVineVolume>) : HomeUiState()
    data class IssuesSuccess(val issues: List<ComicVineIssueSummary>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val prefsRepository: UserPreferencesRepository) : ViewModel() {
    private val comicVine = ComicVineApi()

    private val _selectedTab = MutableStateFlow(HomeTab.Series)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedSeries: List<ComicVineVolume>? = null
    private var cachedIssues: List<ComicVineIssueSummary>? = null

    init {
        viewModelScope.launch {
            val savedTabName = prefsRepository.homeDefaultTab.first()
            val savedTab = HomeTab.entries.find { it.name == savedTabName } ?: HomeTab.Series
            _selectedTab.value = savedTab
            loadForTab(savedTab)
        }
    }

    fun selectTab(tab: HomeTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        viewModelScope.launch { prefsRepository.setHomeDefaultTab(tab.name) }
        loadForTab(tab)
    }

    private fun loadForTab(tab: HomeTab) {
        when (tab) {
            HomeTab.Series -> cachedSeries?.let { _uiState.value = HomeUiState.SeriesSuccess(it) } ?: loadSeries()
            HomeTab.Issues -> cachedIssues?.let { _uiState.value = HomeUiState.IssuesSuccess(it) } ?: loadIssues()
        }
    }

    private fun loadSeries() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val series = comicVine.getPopularSeries(limit = HOME_SERIES_LIMIT)
                cachedSeries = series
                _uiState.value = HomeUiState.SeriesSuccess(series)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.toUserFacingNetworkMessage("ComicVine", "Failed to load series")
                )
            }
        }
    }

    private fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val issues = comicVine.getRecentIssues().filter { it.volume?.name?.isNotBlank() == true }
                cachedIssues = issues
                _uiState.value = HomeUiState.IssuesSuccess(issues)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.toUserFacingNetworkMessage("ComicVine", "Failed to load issues")
                )
            }
        }
    }

    private companion object {
        const val HOME_SERIES_LIMIT = 12
    }
}

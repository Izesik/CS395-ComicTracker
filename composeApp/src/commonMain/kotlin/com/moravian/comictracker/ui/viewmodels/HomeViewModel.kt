package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.network.ComicRepository
import com.moravian.comictracker.network.ComicVineIssue
import com.moravian.comictracker.network.ComicVineVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeTab { Volumes, Issues }

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class VolumesSuccess(val volumes: List<ComicVineVolume>) : HomeUiState()
    data class IssuesSuccess(val issues: List<ComicVineIssue>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

// Client-side safety net: drop anything explicitly tagged as Mature (ComicVine results)
private val ComicVineVolume.isMature get() =
    contentRating?.lowercase()?.contains("mature") == true

private val ComicVineIssue.isMature get() =
    contentRating?.lowercase()?.contains("mature") == true

class HomeViewModel : ViewModel() {
    private val repo = ComicRepository()

    private val _selectedTab = MutableStateFlow(HomeTab.Volumes)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var cachedVolumes: List<ComicVineVolume>? = null
    private var cachedIssues: List<ComicVineIssue>? = null

    init {
        loadVolumes()
    }

    fun selectTab(tab: HomeTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        when (tab) {
            HomeTab.Volumes -> cachedVolumes?.let { _uiState.value = HomeUiState.VolumesSuccess(it) } ?: loadVolumes()
            HomeTab.Issues -> cachedIssues?.let { _uiState.value = HomeUiState.IssuesSuccess(it) } ?: loadIssues()
        }
    }

    private fun loadVolumes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val safe = repo.getHomeVolumes().filter { !it.isMature }
                cachedVolumes = safe
                _uiState.value = HomeUiState.VolumesSuccess(safe)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load volumes")
            }
        }
    }

    private fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val safe = repo.getHomeIssues().filter { !it.isMature }
                cachedIssues = safe
                _uiState.value = HomeUiState.IssuesSuccess(safe)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load issues")
            }
        }
    }
}

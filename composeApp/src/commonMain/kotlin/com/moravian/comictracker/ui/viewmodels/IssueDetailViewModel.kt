package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicIssueEntity
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

sealed class IssueDetailUiState {
    data object Loading : IssueDetailUiState()
    data class Success(val issue: ComicVineIssue) : IssueDetailUiState()
    data class Error(val message: String) : IssueDetailUiState()
}

class IssueDetailViewModel(
    private val issueId: Int,
    private val dao: ComicDao
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)
    val addState: StateFlow<AddCollectionState> = _addState.asStateFlow()

    init {
        loadIssue()
        checkIfInCollection()
    }

    private fun loadIssue() {
        viewModelScope.launch {
            _uiState.value = IssueDetailUiState.Loading
            try {
                val response = api.getIssue(issueId)
                _uiState.value = IssueDetailUiState.Success(response.results)
            } catch (e: Exception) {
                _uiState.value = IssueDetailUiState.Error(e.message ?: "Failed to load issue")
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getIssueByComicvineId(issueId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
        }
    }

    fun addToCollection() {
        val issue = (uiState.value as? IssueDetailUiState.Success)?.issue ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding

            val seriesId = if (issue.volume != null) {
                val existing = dao.getSeriesByComicvineId(issue.volume.id)
                existing?.id ?: dao.insertSeries(
                    SeriesEntity(
                        comicvineId = issue.volume.id,
                        title = issue.volume.name,
                        coverImageUrl = issue.image?.mediumUrl
                    )
                )
            } else {
                dao.insertSeries(
                    SeriesEntity(
                        comicvineId = issueId,
                        title = issue.name ?: "Issue #${issue.issueNumber}",
                        coverImageUrl = issue.image?.mediumUrl
                    )
                )
            }

            dao.insertComicIssue(
                ComicIssueEntity(
                    comicvineId = issue.id,
                    seriesId = seriesId,
                    issueNumber = issue.issueNumber.toIntOrNull() ?: 0,
                    title = issue.name ?: "#${issue.issueNumber}",
                    coverImageUrl = issue.image?.mediumUrl
                )
            )
            _addState.value = AddCollectionState.Added
        }
    }

    companion object {
        fun factory(issueId: Int, database: ComicTrackerDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    IssueDetailViewModel(issueId, database.comicDao()) as T
            }
    }
}

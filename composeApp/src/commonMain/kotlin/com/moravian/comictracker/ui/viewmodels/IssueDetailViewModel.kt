package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicIssueEntity
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.network.MetronApi
import com.moravian.comictracker.network.MetronIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

sealed class IssueDetailUiState {
    data object Loading : IssueDetailUiState()
    data class Success(val issue: MetronIssue) : IssueDetailUiState()
    data class Error(val message: String) : IssueDetailUiState()
}

class IssueDetailViewModel(
    private val issueId: Int,
    private val dao: ComicDao
) : ViewModel() {
    private val api = MetronApi()

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
                val issue = api.getIssue(issueId)
                _uiState.value = IssueDetailUiState.Success(issue)
            } catch (e: Exception) {
                _uiState.value = IssueDetailUiState.Error(e.message ?: "Failed to load issue")
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getIssueByMetronId(issueId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
        }
    }

    fun addToCollection() {
        val issue = (uiState.value as? IssueDetailUiState.Success)?.issue ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding

            val seriesId = if (issue.series != null) {
                val existing = dao.getSeriesByMetronId(issue.series.id)
                existing?.id ?: dao.insertSeries(
                    SeriesEntity(
                        metronId = issue.series.id,
                        title = issue.series.name,
                        publisher = issue.publisher?.name,
                        coverImageUrl = issue.image
                    )
                )
            } else {
                dao.insertSeries(
                    SeriesEntity(
                        metronId = issueId,
                        title = "Issue #${issue.number}",
                        coverImageUrl = issue.image
                    )
                )
            }

            dao.insertComicIssue(
                ComicIssueEntity(
                    metronId = issue.id,
                    seriesId = seriesId,
                    issueNumber = issue.number.toIntOrNull() ?: 0,
                    title = "#${issue.number}",
                    coverImageUrl = issue.image
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

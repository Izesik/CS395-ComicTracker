package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicIssueEntity
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.CreatorEntity
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssue
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.network.toUserFacingNetworkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** Possible UI states for the issue detail screen. */
sealed class IssueDetailUiState {
    /** Issue data is being loaded from the API. */
    data object Loading : IssueDetailUiState()

    /** Issue loaded successfully. */
    data class Success(
        val issue: ComicVineIssue,
    ) : IssueDetailUiState()

    /** A network or API error occurred; [message] is suitable for display. */
    data class Error(
        val message: String,
    ) : IssueDetailUiState()
}

/**
 * ViewModel for the Issue Detail screen.
 *
 * Fetches full issue data from ComicVine and manages add/remove collection operations,
 * including creating a parent series row if none exists locally yet.
 */
class IssueDetailViewModel(
    private val issueId: Int,
    private val dao: ComicDao,
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)

    /** Current state of the issue detail content area. */
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)

    /** Current state of the add/remove collection button. */
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
                _uiState.value =
                    IssueDetailUiState.Error(
                        e.toUserFacingNetworkMessage("ComicVine", "Failed to load issue"),
                    )
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getIssueByComicVineId(issueId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
        }
    }

    /** Removes this issue from the local collection. */
    fun removeFromCollection() {
        viewModelScope.launch {
            _addState.value = AddCollectionState.Removing
            val existing = dao.getIssueByComicVineId(issueId)
            if (existing != null) dao.deleteComicIssue(existing.id)
            _addState.value = AddCollectionState.Idle
        }
    }

    /**
     * Saves this issue to the local collection, creating a parent series row if needed.
     *
     * Creator credits are also persisted for each person listed in the issue's credits.
     */
    fun addToCollection() {
        val issue = (uiState.value as? IssueDetailUiState.Success)?.issue ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding

            val seriesId =
                if (issue.volume != null) {
                    val existing = dao.getSeriesByComicVineId(issue.volume.id)
                    existing?.id ?: dao.insertSeries(
                        SeriesEntity(
                            comicvineId = issue.volume.id,
                            title = issue.volume.name,
                            coverImageUrl = issue.image?.coverUrl(),
                        ),
                    )
                } else {
                    dao.insertSeries(
                        SeriesEntity(
                            comicvineId = issueId,
                            title = "Issue #${issue.issueNumber}",
                            coverImageUrl = issue.image?.coverUrl(),
                        ),
                    )
                }

            val issueRowId =
                dao.insertComicIssue(
                    ComicIssueEntity(
                        comicvineId = issue.id,
                        seriesId = seriesId,
                        issueNumber = issue.issueNumber.toIntOrNull() ?: 0,
                        title = "#${issue.issueNumber}",
                        coverImageUrl = issue.image?.coverUrl(),
                    ),
                )
            issue.personCredits.forEach { credit ->
                dao.insertCreator(
                    CreatorEntity(
                        issueId = issueRowId,
                        comicvineId = credit.id,
                        name = credit.name,
                        role = credit.role,
                    ),
                )
            }
            _addState.value = AddCollectionState.Added
        }
    }

    companion object {
        /** Creates a factory that injects [issueId] and [database]. */
        fun factory(
            issueId: Int,
            database: ComicTrackerDatabase,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T = IssueDetailViewModel(issueId, database.comicDao()) as T
            }
    }
}

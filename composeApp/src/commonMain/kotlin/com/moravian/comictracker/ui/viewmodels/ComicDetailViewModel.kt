package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.CreatorEntity
import com.moravian.comictracker.data.IssuesCollectionFilter
import com.moravian.comictracker.data.IssuesSortOrder
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssueSummary
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.network.toUserFacingNetworkMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** Possible UI states for the series detail screen. */
sealed class ComicDetailUiState {
    /** Series data is being loaded from the API. */
    data object Loading : ComicDetailUiState()

    /** Series loaded successfully. */
    data class Success(
        val series: ComicVineVolume,
    ) : ComicDetailUiState()

    /** A network or API error occurred; [message] is suitable for display. */
    data class Error(
        val message: String,
    ) : ComicDetailUiState()
}

/**
 * ViewModel for the Comic Detail screen.
 *
 * Loads series and issue data from ComicVine, observes local collection state from Room,
 * and exposes a filtered and sorted [displayedIssues] list driven by persisted preferences.
 */
class ComicDetailViewModel(
    private val seriesId: Int,
    private val dao: ComicDao,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)

    /** Current state of the series detail content area. */
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)

    /** Current state of the add/remove collection button. */
    val addState: StateFlow<AddCollectionState> = _addState.asStateFlow()

    private val _allIssues = MutableStateFlow<List<ComicVineIssueSummary>>(emptyList())

    private val _collectionIssueIds = MutableStateFlow<Set<Int>>(emptySet())

    /** Set of ComicVine issue IDs the user has saved locally, used to show "IN COLLECTION" badges. */
    val collectionIssueIds: StateFlow<Set<Int>> = _collectionIssueIds.asStateFlow()

    private val _seriesCreators = MutableStateFlow<List<CreatorEntity>>(emptyList())

    /** Deduplicated creator credits for the series, loaded from local storage. */
    val seriesCreators: StateFlow<List<CreatorEntity>> = _seriesCreators.asStateFlow()

    /** Persisted sort order for the issues list. */
    val issuesSortOrder: StateFlow<IssuesSortOrder> =
        prefsRepository.issuesSortOrder
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IssuesSortOrder.NUMBER_ASC)

    /** Persisted collection filter for the issues list. */
    val issuesCollectionFilter: StateFlow<IssuesCollectionFilter> =
        prefsRepository.issuesCollectionFilter
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IssuesCollectionFilter.ALL)

    /** Issues filtered by [issuesCollectionFilter] and sorted by [issuesSortOrder]. */
    val displayedIssues: StateFlow<List<ComicVineIssueSummary>> =
        combine(
            _allIssues,
            _collectionIssueIds,
            issuesSortOrder,
            issuesCollectionFilter,
        ) { issues, collectionIds, sort, filter ->
            val filtered =
                when (filter) {
                    IssuesCollectionFilter.ALL -> issues
                    IssuesCollectionFilter.IN_COLLECTION -> issues.filter { it.id in collectionIds }
                    IssuesCollectionFilter.NOT_IN_COLLECTION -> issues.filter { it.id !in collectionIds }
                }
            when (sort) {
                IssuesSortOrder.NUMBER_ASC -> filtered.sortedBy { it.issueNumber.toDoubleOrNull() ?: 0.0 }
                IssuesSortOrder.NUMBER_DESC -> filtered.sortedByDescending { it.issueNumber.toDoubleOrNull() ?: 0.0 }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var collectionDataJob: Job? = null

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
                _uiState.value =
                    ComicDetailUiState.Error(
                        e.toUserFacingNetworkMessage("ComicVine", "Failed to load series details"),
                    )
            }
        }
    }

    private fun loadIssues() {
        viewModelScope.launch {
            try {
                _allIssues.value = api.getIssuesByVolume(seriesId)
            } catch (_: Exception) {
                // Issues list is optional — failure is silent
            }
        }
    }

    private fun checkIfInCollection() {
        viewModelScope.launch {
            val existing = dao.getSeriesByComicVineId(seriesId)
            _addState.value = if (existing != null) AddCollectionState.InCollection else AddCollectionState.Idle
            if (existing != null) observeCollectionData(existing.id)
        }
    }

    private fun observeCollectionData(localSeriesId: Long) {
        collectionDataJob?.cancel()
        collectionDataJob =
            viewModelScope.launch {
                launch {
                    dao.getComicIssuesForSeries(localSeriesId).collect { list ->
                        _collectionIssueIds.value = list.map { it.comicvineId }.toSet()
                    }
                }
                launch {
                    dao.getCreatorsForSeries(localSeriesId).collect { creators ->
                        _seriesCreators.value = creators
                    }
                }
            }
    }

    /** Toggles the issue sort order between ascending and descending and persists the choice. */
    fun toggleSortOrder() {
        viewModelScope.launch {
            val next =
                if (issuesSortOrder.value == IssuesSortOrder.NUMBER_ASC) {
                    IssuesSortOrder.NUMBER_DESC
                } else {
                    IssuesSortOrder.NUMBER_ASC
                }
            prefsRepository.setIssuesSortOrder(next)
        }
    }

    /** Sets the active collection [filter] for the issues list and persists it. */
    fun setCollectionFilter(filter: IssuesCollectionFilter) {
        viewModelScope.launch { prefsRepository.setIssuesCollectionFilter(filter) }
    }

    /** Saves the current series and all its issues to the local collection. */
    fun addToCollection() {
        val series = (uiState.value as? ComicDetailUiState.Success)?.series ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding
            val newId =
                dao.insertSeries(
                    SeriesEntity(
                        comicvineId = series.id,
                        title = series.name,
                        publisher = series.publisher?.name,
                        coverImageUrl = series.image?.coverUrl(),
                    ),
                )
            observeCollectionData(newId)
            _addState.value = AddCollectionState.Added
        }
    }

    /** Removes the current series and all its issues from the local collection. */
    fun removeFromCollection() {
        viewModelScope.launch {
            _addState.value = AddCollectionState.Removing
            val existing = dao.getSeriesByComicVineId(seriesId)
            if (existing != null) dao.deleteSeries(existing.id)
            collectionDataJob?.cancel()
            collectionDataJob = null
            _collectionIssueIds.value = emptySet()
            _seriesCreators.value = emptyList()
            _addState.value = AddCollectionState.Idle
        }
    }

    companion object {
        /** Creates a factory that injects [seriesId], [database], and [prefsRepository]. */
        fun factory(
            seriesId: Int,
            database: ComicTrackerDatabase,
            prefsRepository: UserPreferencesRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T = ComicDetailViewModel(seriesId, database.comicDao(), prefsRepository) as T
            }
    }
}

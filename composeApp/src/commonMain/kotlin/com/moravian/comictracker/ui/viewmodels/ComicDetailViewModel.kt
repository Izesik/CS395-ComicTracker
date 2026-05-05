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

sealed class ComicDetailUiState {
    data object Loading : ComicDetailUiState()
    data class Success(val series: ComicVineVolume) : ComicDetailUiState()
    data class Error(val message: String) : ComicDetailUiState()
}

class ComicDetailViewModel(
    private val seriesId: Int,
    private val dao: ComicDao,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddCollectionState>(AddCollectionState.Checking)
    val addState: StateFlow<AddCollectionState> = _addState.asStateFlow()

    private val _allIssues = MutableStateFlow<List<ComicVineIssueSummary>>(emptyList())

    private val _collectionIssueIds = MutableStateFlow<Set<Int>>(emptySet())
    val collectionIssueIds: StateFlow<Set<Int>> = _collectionIssueIds.asStateFlow()

    private val _seriesCreators = MutableStateFlow<List<CreatorEntity>>(emptyList())
    val seriesCreators: StateFlow<List<CreatorEntity>> = _seriesCreators.asStateFlow()

    val issuesSortOrder: StateFlow<IssuesSortOrder> = prefsRepository.issuesSortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IssuesSortOrder.NUMBER_ASC)

    val issuesCollectionFilter: StateFlow<IssuesCollectionFilter> = prefsRepository.issuesCollectionFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IssuesCollectionFilter.ALL)

    val displayedIssues: StateFlow<List<ComicVineIssueSummary>> = combine(
        _allIssues, _collectionIssueIds, issuesSortOrder, issuesCollectionFilter
    ) { issues, collectionIds, sort, filter ->
        val filtered = when (filter) {
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
                _uiState.value = ComicDetailUiState.Error(
                    e.toUserFacingNetworkMessage("ComicVine", "Failed to load series details")
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
        collectionDataJob = viewModelScope.launch {
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

    fun toggleSortOrder() {
        viewModelScope.launch {
            val next = if (issuesSortOrder.value == IssuesSortOrder.NUMBER_ASC)
                IssuesSortOrder.NUMBER_DESC else IssuesSortOrder.NUMBER_ASC
            prefsRepository.setIssuesSortOrder(next)
        }
    }

    fun setCollectionFilter(filter: IssuesCollectionFilter) {
        viewModelScope.launch { prefsRepository.setIssuesCollectionFilter(filter) }
    }

    fun addToCollection() {
        val series = (uiState.value as? ComicDetailUiState.Success)?.series ?: return
        viewModelScope.launch {
            _addState.value = AddCollectionState.Adding
            val newId = dao.insertSeries(
                SeriesEntity(
                    comicvineId = series.id,
                    title = series.name,
                    publisher = series.publisher?.name,
                    coverImageUrl = series.image?.coverUrl()
                )
            )
            observeCollectionData(newId)
            _addState.value = AddCollectionState.Added
        }
    }

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
        fun factory(
            seriesId: Int,
            database: ComicTrackerDatabase,
            prefsRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    ComicDetailViewModel(seriesId, database.comicDao(), prefsRepository) as T
            }
    }
}

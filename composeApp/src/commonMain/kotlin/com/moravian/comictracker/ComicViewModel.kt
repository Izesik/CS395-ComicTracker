package com.moravian.comictracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicIssueEntity
import com.moravian.comictracker.data.ReadStatus
import com.moravian.comictracker.data.SeriesEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ComicViewModel(private val dao: ComicDao) : ViewModel() {

    val seriesList: StateFlow<List<SeriesEntity>> = dao.getAllSeries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getIssuesForSeries(seriesId: Long) = dao.getComicIssuesForSeries(seriesId)

    fun addSeries(series: SeriesEntity) {
        viewModelScope.launch { dao.insertSeries(series) }
    }

    fun addIssue(issue: ComicIssueEntity) {
        viewModelScope.launch { dao.insertComicIssue(issue) }
    }

    fun updateReadStatus(issueId: Long, status: ReadStatus) {
        viewModelScope.launch { dao.updateReadStatus(issueId, status) }
    }

    fun deleteIssue(issueId: Long) {
        viewModelScope.launch { dao.deleteComicIssue(issueId) }
    }

    fun deleteSeries(seriesId: Long) {
        viewModelScope.launch { dao.deleteSeries(seriesId) }
    }
}

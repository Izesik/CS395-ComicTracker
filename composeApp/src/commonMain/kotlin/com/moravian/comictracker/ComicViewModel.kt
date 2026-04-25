package com.moravian.comictracker

import androidx.lifecycle.ViewModel
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import kotlinx.coroutines.flow.MutableStateFlow

class ComicViewModel(private val comicDatabase: ComicTrackerDatabase): ViewModel() {
    val seriesList = MutableStateFlow<List<SeriesEntity>>(emptyList())

    init {
        loadSeries()
    }

    private fun loadSeries() {
        TODO("Fetch series from api and update database")
    }
}
package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.data.CollectionLayout
import com.moravian.comictracker.data.ComicDao
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class CollectionViewModel(
    dao: ComicDao,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val series: StateFlow<List<SeriesEntity>> = dao.getAllSeries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionLayout: StateFlow<CollectionLayout> = prefsRepository.collectionLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionLayout.GRID)

    fun toggleLayout() {
        viewModelScope.launch {
            val next = if (collectionLayout.value == CollectionLayout.GRID) CollectionLayout.LIST else CollectionLayout.GRID
            prefsRepository.setCollectionLayout(next)
        }
    }

    companion object {
        fun factory(database: ComicTrackerDatabase, prefsRepository: UserPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    CollectionViewModel(database.comicDao(), prefsRepository) as T
            }
    }
}

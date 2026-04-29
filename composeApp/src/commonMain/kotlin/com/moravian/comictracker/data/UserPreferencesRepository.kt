package com.moravian.comictracker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class CollectionLayout { GRID, LIST }

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private val layoutKey = stringPreferencesKey("collection_layout")

    val collectionLayout: Flow<CollectionLayout> = dataStore.data.map { prefs ->
        when (prefs[layoutKey]) {
            CollectionLayout.LIST.name -> CollectionLayout.LIST
            else -> CollectionLayout.GRID
        }
    }

    suspend fun setCollectionLayout(layout: CollectionLayout) {
        dataStore.edit { it[layoutKey] = layout.name }
    }
}

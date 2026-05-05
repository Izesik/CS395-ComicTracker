package com.moravian.comictracker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class CollectionLayout { GRID, LIST }
enum class CollectionSort { TITLE, DATE_ADDED }
enum class SearchLayout { GRID, LIST }
enum class IssuesSortOrder { NUMBER_ASC, NUMBER_DESC }
enum class IssuesCollectionFilter { ALL, IN_COLLECTION, NOT_IN_COLLECTION }

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val collectionLayoutKey = stringPreferencesKey("collection_layout")
    private val collectionSortKey = stringPreferencesKey("collection_sort")
    private val searchLayoutKey = stringPreferencesKey("search_layout")
    private val issuesSortOrderKey = stringPreferencesKey("issues_sort_order")
    private val issuesCollectionFilterKey = stringPreferencesKey("issues_collection_filter")
    private val homeDefaultTabKey = stringPreferencesKey("home_default_tab")

    val collectionLayout: Flow<CollectionLayout> = dataStore.data.map { prefs ->
        when (prefs[collectionLayoutKey]) {
            CollectionLayout.LIST.name -> CollectionLayout.LIST
            else -> CollectionLayout.GRID
        }
    }

    suspend fun setCollectionLayout(layout: CollectionLayout) {
        dataStore.edit { it[collectionLayoutKey] = layout.name }
    }

    val collectionSort: Flow<CollectionSort> = dataStore.data.map { prefs ->
        when (prefs[collectionSortKey]) {
            CollectionSort.DATE_ADDED.name -> CollectionSort.DATE_ADDED
            else -> CollectionSort.TITLE
        }
    }

    suspend fun setCollectionSort(sort: CollectionSort) {
        dataStore.edit { it[collectionSortKey] = sort.name }
    }

    val searchLayout: Flow<SearchLayout> = dataStore.data.map { prefs ->
        when (prefs[searchLayoutKey]) {
            SearchLayout.LIST.name -> SearchLayout.LIST
            else -> SearchLayout.GRID
        }
    }

    suspend fun setSearchLayout(layout: SearchLayout) {
        dataStore.edit { it[searchLayoutKey] = layout.name }
    }

    val issuesSortOrder: Flow<IssuesSortOrder> = dataStore.data.map { prefs ->
        when (prefs[issuesSortOrderKey]) {
            IssuesSortOrder.NUMBER_DESC.name -> IssuesSortOrder.NUMBER_DESC
            else -> IssuesSortOrder.NUMBER_ASC
        }
    }

    suspend fun setIssuesSortOrder(order: IssuesSortOrder) {
        dataStore.edit { it[issuesSortOrderKey] = order.name }
    }

    val issuesCollectionFilter: Flow<IssuesCollectionFilter> = dataStore.data.map { prefs ->
        when (prefs[issuesCollectionFilterKey]) {
            IssuesCollectionFilter.IN_COLLECTION.name -> IssuesCollectionFilter.IN_COLLECTION
            IssuesCollectionFilter.NOT_IN_COLLECTION.name -> IssuesCollectionFilter.NOT_IN_COLLECTION
            else -> IssuesCollectionFilter.ALL
        }
    }

    suspend fun setIssuesCollectionFilter(filter: IssuesCollectionFilter) {
        dataStore.edit { it[issuesCollectionFilterKey] = filter.name }
    }

    val homeDefaultTab: Flow<String> = dataStore.data.map { prefs ->
        prefs[homeDefaultTabKey] ?: "Series"
    }

    suspend fun setHomeDefaultTab(tabName: String) {
        dataStore.edit { it[homeDefaultTabKey] = tabName }
    }
}

package com.moravian.comictracker.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Layout option for the collection and search grids. */
enum class CollectionLayout {
    /** Items arranged in a cover-art grid. */
    GRID,

    /** Items arranged as a single-column list with metadata. */
    LIST,
}

/** Sort order for the user's saved series collection. */
enum class CollectionSort {
    /** Sort alphabetically by series title. */
    TITLE,

    /** Sort by the order items were added, newest first. */
    DATE_ADDED,
}

/** Layout option for the search results screen. */
enum class SearchLayout {
    /** Results arranged in a cover-art grid. */
    GRID,

    /** Results arranged as a single-column list with metadata. */
    LIST,
}

/** Sort order for the issues list on the series detail screen. */
enum class IssuesSortOrder {
    /** Sort issues from lowest to highest issue number. */
    NUMBER_ASC,

    /** Sort issues from highest to lowest issue number. */
    NUMBER_DESC,
}

/** Filter for which issues are shown in the series detail issues list. */
enum class IssuesCollectionFilter {
    /** Show all issues for the series. */
    ALL,

    /** Show only issues the user has saved locally. */
    IN_COLLECTION,

    /** Show only issues the user has not yet saved. */
    NOT_IN_COLLECTION,
}

/**
 * Persists user UI preferences using DataStore.
 *
 * Each preference is exposed as a [Flow] and updated via a suspending setter.
 * Six preferences are stored: collection layout, collection sort, search layout,
 * issues sort order, issues collection filter, and the default home tab.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val collectionLayoutKey = stringPreferencesKey("collection_layout")
    private val collectionSortKey = stringPreferencesKey("collection_sort")
    private val searchLayoutKey = stringPreferencesKey("search_layout")
    private val issuesSortOrderKey = stringPreferencesKey("issues_sort_order")
    private val issuesCollectionFilterKey = stringPreferencesKey("issues_collection_filter")
    private val homeDefaultTabKey = stringPreferencesKey("home_default_tab")

    /** Emits the user's preferred layout for the collection screen. Defaults to [CollectionLayout.GRID]. */
    val collectionLayout: Flow<CollectionLayout> =
        dataStore.data.map { prefs ->
            when (prefs[collectionLayoutKey]) {
                CollectionLayout.LIST.name -> CollectionLayout.LIST
                else -> CollectionLayout.GRID
            }
        }

    /** Persists the collection [layout] preference. */
    suspend fun setCollectionLayout(layout: CollectionLayout) {
        dataStore.edit { it[collectionLayoutKey] = layout.name }
    }

    /** Emits the user's preferred sort order for the collection screen. Defaults to [CollectionSort.TITLE]. */
    val collectionSort: Flow<CollectionSort> =
        dataStore.data.map { prefs ->
            when (prefs[collectionSortKey]) {
                CollectionSort.DATE_ADDED.name -> CollectionSort.DATE_ADDED
                else -> CollectionSort.TITLE
            }
        }

    /** Persists the collection [sort] preference. */
    suspend fun setCollectionSort(sort: CollectionSort) {
        dataStore.edit { it[collectionSortKey] = sort.name }
    }

    /** Emits the user's preferred layout for the search results screen. Defaults to [SearchLayout.GRID]. */
    val searchLayout: Flow<SearchLayout> =
        dataStore.data.map { prefs ->
            when (prefs[searchLayoutKey]) {
                SearchLayout.LIST.name -> SearchLayout.LIST
                else -> SearchLayout.GRID
            }
        }

    /** Persists the search results [layout] preference. */
    suspend fun setSearchLayout(layout: SearchLayout) {
        dataStore.edit { it[searchLayoutKey] = layout.name }
    }

    /** Emits the user's preferred sort order for the issues list. Defaults to [IssuesSortOrder.NUMBER_ASC]. */
    val issuesSortOrder: Flow<IssuesSortOrder> =
        dataStore.data.map { prefs ->
            when (prefs[issuesSortOrderKey]) {
                IssuesSortOrder.NUMBER_DESC.name -> IssuesSortOrder.NUMBER_DESC
                else -> IssuesSortOrder.NUMBER_ASC
            }
        }

    /** Persists the issues sort [order] preference. */
    suspend fun setIssuesSortOrder(order: IssuesSortOrder) {
        dataStore.edit { it[issuesSortOrderKey] = order.name }
    }

    /** Emits the user's active collection filter for the issues list. Defaults to [IssuesCollectionFilter.ALL]. */
    val issuesCollectionFilter: Flow<IssuesCollectionFilter> =
        dataStore.data.map { prefs ->
            when (prefs[issuesCollectionFilterKey]) {
                IssuesCollectionFilter.IN_COLLECTION.name -> IssuesCollectionFilter.IN_COLLECTION
                IssuesCollectionFilter.NOT_IN_COLLECTION.name -> IssuesCollectionFilter.NOT_IN_COLLECTION
                else -> IssuesCollectionFilter.ALL
            }
        }

    /** Persists the issues collection [filter] preference. */
    suspend fun setIssuesCollectionFilter(filter: IssuesCollectionFilter) {
        dataStore.edit { it[issuesCollectionFilterKey] = filter.name }
    }

    /** Emits the name of the tab that should be selected when the home screen opens. Defaults to "Series". */
    val homeDefaultTab: Flow<String> =
        dataStore.data.map { prefs ->
            prefs[homeDefaultTabKey] ?: "Series"
        }

    /** Persists the home default [tabName] preference. */
    suspend fun setHomeDefaultTab(tabName: String) {
        dataStore.edit { it[homeDefaultTabKey] = tabName }
    }
}

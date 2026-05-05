package com.moravian.comictracker.ui.viewmodels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import coil3.compose.AsyncImage
import com.moravian.comictracker.data.SearchLayout
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.network.toUserFacingNetworkMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private val CardBackground = Color(0xFF1E1E1E)
private val CoverPlaceholder = Color(0xFF2A2A2A)
private val TextPrimary = Color.White
private val TextMuted = Color(0xFF888888)

/** Possible states for the search results area. */
sealed class SearchUiState {
    /** No search has been performed yet. */
    data object Idle : SearchUiState()
    /** A search request is in flight. */
    data object Loading : SearchUiState()
    /** Search completed with [results]. */
    data class Success(val results: List<ComicVineVolume>) : SearchUiState()
    /** Search failed with a user-facing [message]. */
    data class Error(val message: String) : SearchUiState()
}

/**
 * ViewModel for the Search screen.
 *
 * Persists [searchQuery] across configuration changes via [SavedStateHandle] so the
 * typed text is not lost on rotation or process restoration.
 */
class SearchViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val comicVine = ComicVineApi()

    /** The current text in the search field, restored automatically on config change. */
    var searchQuery by mutableStateOf(savedStateHandle[KEY_QUERY] ?: "")
        private set

    var uiState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set

    /** Persisted layout preference (grid vs. list) for search results. */
    val searchLayout: StateFlow<SearchLayout> = prefsRepository.searchLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchLayout.GRID)

    /** Updates [searchQuery] and persists it in [SavedStateHandle]. */
    fun onQueryChange(newQuery: String) {
        searchQuery = newQuery
        savedStateHandle[KEY_QUERY] = newQuery
    }

    /** Executes the search for the current [searchQuery] against the ComicVine API. */
    fun performSearch() {
        val query = searchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            uiState = SearchUiState.Loading
            try {
                val results = comicVine.searchVolumes(query)
                uiState = SearchUiState.Success(results)
            } catch (e: Exception) {
                uiState = SearchUiState.Error(
                    e.toUserFacingNetworkMessage("ComicVine", "Search failed")
                )
            }
        }
    }

    /** Toggles the result layout between grid and list, persisting the choice. */
    fun toggleSearchLayout() {
        viewModelScope.launch {
            val next = if (searchLayout.value == SearchLayout.GRID) SearchLayout.LIST else SearchLayout.GRID
            prefsRepository.setSearchLayout(next)
        }
    }

    companion object {
        private const val KEY_QUERY = "search_query"

        /** Creates a factory that injects [prefsRepository] and a [SavedStateHandle]. */
        fun factory(prefsRepository: UserPreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    SearchViewModel(prefsRepository, extras.createSavedStateHandle()) as T
            }
    }
}

/** List-row card used when search results are displayed in list layout. */
@Composable
fun SeriesSearchCard(
    result: ComicVineVolume,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp, 84.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CoverPlaceholder),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = result.image?.coverUrl()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = result.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Text(
                    text = result.name,
                    color = TextMuted,
                    fontSize = 9.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            result.publisher?.name?.let {
                Text(
                    text = it,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            result.startYear?.let {
                Text(
                    text = it,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

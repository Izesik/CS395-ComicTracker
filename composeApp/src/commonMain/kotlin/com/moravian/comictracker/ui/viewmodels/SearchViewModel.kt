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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.moravian.comictracker.network.MetronApi
import com.moravian.comictracker.network.MetronSeriesSummary
import kotlinx.coroutines.launch

private val CardBackground = Color(0xFF1E1E1E)
private val CoverPlaceholder = Color(0xFF2A2A2A)
private val TextPrimary = Color.White
private val TextMuted = Color(0xFF888888)

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: List<MetronSeriesSummary>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel : ViewModel() {
    private val metron = MetronApi()

    var searchQuery by mutableStateOf("")
        private set

    var uiState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set

    fun onQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    fun performSearch() {
        val query = searchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            uiState = SearchUiState.Loading
            try {
                val results = metron.searchSeries(query).results
                uiState = SearchUiState.Success(results)
                loadSeriesCovers(query, results)
            } catch (e: Exception) {
                uiState = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    private suspend fun loadSeriesCovers(
        query: String,
        results: List<MetronSeriesSummary>
    ) {
        var enrichedResults = results
        results.take(MAX_SERIES_COVERS).forEach { item ->
            if (!item.image.isNullOrBlank() || searchQuery.trim() != query) return@forEach

            val cover = try {
                metron.getFirstIssueCoverForSeries(item.id)
            } catch (_: Exception) {
                null
            }

            if (!cover.isNullOrBlank() && searchQuery.trim() == query) {
                enrichedResults = enrichedResults.map { result ->
                    if (result.id == item.id) result.copy(image = cover) else result
                }
                uiState = SearchUiState.Success(enrichedResults)
            }
        }
    }

    private companion object {
        const val MAX_SERIES_COVERS = 18
    }
}

@Composable
fun SeriesSearchCard(
    result: MetronSeriesSummary,
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
            if (result.image != null) {
                AsyncImage(
                    model = result.image,
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
            result.yearBegan?.let {
                Text(
                    text = it.toString(),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

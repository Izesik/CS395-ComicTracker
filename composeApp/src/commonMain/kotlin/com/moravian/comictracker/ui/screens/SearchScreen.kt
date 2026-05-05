package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.moravian.comictracker.data.SearchLayout
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.ui.viewmodels.SearchUiState
import com.moravian.comictracker.ui.viewmodels.SearchViewModel
import com.moravian.comictracker.ui.viewmodels.SeriesSearchCard
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.no_results_found
import comictracker.composeapp.generated.resources.screen_search
import comictracker.composeapp.generated.resources.search_comicvine_hint
import comictracker.composeapp.generated.resources.searching_comics
import comictracker.composeapp.generated.resources.switch_to_grid_view
import comictracker.composeapp.generated.resources.switch_to_list_view
import org.jetbrains.compose.resources.stringResource

private val SearchBackground = Color(0xFF0F0F0F)
private val FieldBackground = Color(0xFF1A1A1A)
private val CardBackground = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextMuted = Color(0xFF777777)

/** Full-screen search interface for finding comic series via the ComicVine API. */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onComicClick: (Int) -> Unit
) {
    val searchLayout by viewModel.searchLayout.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.screen_search),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::toggleSearchLayout) {
                Icon(
                    imageVector = if (searchLayout == SearchLayout.GRID) Icons.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = if (searchLayout == SearchLayout.GRID) stringResource(Res.string.switch_to_list_view) else stringResource(Res.string.switch_to_grid_view),
                    tint = TextMuted
                )
            }
        }

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            TextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.searching_comics), color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = TextMuted,
                    unfocusedLeadingIconColor = TextMuted,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() })
            )
        }

        when (val state = viewModel.uiState) {
            is SearchUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.search_comicvine_hint), color = TextMuted, fontSize = 14.sp)
                }
            }
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary)
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.no_results_found, viewModel.searchQuery),
                            color = TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else when (searchLayout) {
                    SearchLayout.LIST -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.results) { result ->
                            SeriesSearchCard(result, onClick = { onComicClick(result.id) })
                        }
                    }
                    SearchLayout.GRID -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.results) { result ->
                            SearchGridItem(result, onClick = { onComicClick(result.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGridItem(result: ComicVineVolume, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(6.dp))
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = result.image?.coverUrl()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = result.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = result.name,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (result.startYear != null) {
            Text(
                text = result.startYear,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

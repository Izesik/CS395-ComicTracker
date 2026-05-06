package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moravian.comictracker.data.CollectionLayout
import com.moravian.comictracker.data.CollectionSort
import com.moravian.comictracker.data.SeriesEntity
import com.moravian.comictracker.ui.components.ComicCoverGridItem
import com.moravian.comictracker.ui.components.ComicListRow
import com.moravian.comictracker.ui.viewmodels.CollectionViewModel
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.no_comics_on_shelf
import comictracker.composeapp.generated.resources.shelf_label
import comictracker.composeapp.generated.resources.sorted_by_date_added
import comictracker.composeapp.generated.resources.sorted_by_title
import comictracker.composeapp.generated.resources.switch_to_grid_view
import comictracker.composeapp.generated.resources.switch_to_list_view
import org.jetbrains.compose.resources.stringResource

private val CollectionBackground = Color(0xFF0F0F0F)
private val TextPrimary = Color.White
private val TextMuted = Color(0xFF777777)

/** Displays the user's saved comic series collection with sort and layout controls. */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onSeriesClick: (Int) -> Unit = {},
) {
    val series by viewModel.series.collectAsStateWithLifecycle()
    val layout by viewModel.collectionLayout.collectAsStateWithLifecycle()
    val sort by viewModel.collectionSort.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CollectionBackground),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.shelf_label),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f),
            )
            val sortTint by animateColorAsState(
                targetValue = if (sort == CollectionSort.DATE_ADDED) TextPrimary else TextMuted,
                animationSpec = tween(durationMillis = 200),
            )
            IconButton(onClick = viewModel::toggleSort) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription =
                        if (sort ==
                            CollectionSort.TITLE
                        ) {
                            stringResource(Res.string.sorted_by_title)
                        } else {
                            stringResource(Res.string.sorted_by_date_added)
                        },
                    tint = sortTint,
                )
            }
            IconButton(onClick = viewModel::toggleLayout) {
                Icon(
                    imageVector = if (layout == CollectionLayout.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription =
                        if (layout ==
                            CollectionLayout.GRID
                        ) {
                            stringResource(Res.string.switch_to_list_view)
                        } else {
                            stringResource(Res.string.switch_to_grid_view)
                        },
                    tint = TextMuted,
                )
            }
        }

        if (series.isEmpty()) {
            EmptyShelf()
        } else {
            AnimatedContent(
                targetState = layout,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                modifier = Modifier.fillMaxSize(),
            ) { currentLayout ->
                when (currentLayout) {
                    CollectionLayout.GRID -> GridShelf(series, onSeriesClick)
                    CollectionLayout.LIST -> ListShelf(series, onSeriesClick)
                }
            }
        }
    }
}

@Composable
private fun GridShelf(
    series: List<SeriesEntity>,
    onSeriesClick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(series, key = { it.id }) { s ->
            ComicCoverGridItem(
                title = s.title,
                subtitle = s.publisher,
                imageUrl = s.coverImageUrl,
                onClick = { onSeriesClick(s.comicvineId) },
            )
        }
    }
}

@Composable
private fun ListShelf(
    series: List<SeriesEntity>,
    onSeriesClick: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(series, key = { it.id }) { s ->
            ComicListRow(
                title = s.title,
                subtitle = s.publisher,
                tertiary = null,
                imageUrl = s.coverImageUrl,
                onClick = { onSeriesClick(s.comicvineId) },
                imageWidth = 50.dp,
                imageHeight = 70.dp,
            )
        }
    }
}

@Composable
private fun EmptyShelf() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = stringResource(Res.string.no_comics_on_shelf),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}

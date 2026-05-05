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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.CreatorEntity
import com.moravian.comictracker.data.IssuesCollectionFilter
import com.moravian.comictracker.data.IssuesSortOrder
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.network.ComicVineIssueSummary
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.ui.components.PlatformBackButton
import com.moravian.comictracker.ui.viewmodels.AddCollectionState
import com.moravian.comictracker.ui.viewmodels.ComicDetailUiState
import com.moravian.comictracker.ui.viewmodels.ComicDetailViewModel
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.add_to_collection
import comictracker.composeapp.generated.resources.added_to_collection
import comictracker.composeapp.generated.resources.creators_label
import comictracker.composeapp.generated.resources.filter_all
import comictracker.composeapp.generated.resources.filter_in_collection
import comictracker.composeapp.generated.resources.filter_not_in_collection
import comictracker.composeapp.generated.resources.in_collection_badge
import comictracker.composeapp.generated.resources.issues_count_label
import comictracker.composeapp.generated.resources.issues_label
import comictracker.composeapp.generated.resources.loading
import comictracker.composeapp.generated.resources.no_issues_match_filter
import comictracker.composeapp.generated.resources.remove_from_collection
import comictracker.composeapp.generated.resources.series_type_label
import comictracker.composeapp.generated.resources.sort_ascending_cd
import comictracker.composeapp.generated.resources.sort_descending_cd
import comictracker.composeapp.generated.resources.view_on_comicvine
import org.jetbrains.compose.resources.stringResource

private val ScreenBackground = Color(0xFF0F0F0F)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val BadgeGreen = Color(0xFF2E7D32)

/** Detailed view for a comic series, including hero art, description, creators, and issues grid. */
@Composable
fun ComicDetailScreen(
    seriesId: Int,
    onBack: () -> Unit,
    database: ComicTrackerDatabase,
    prefsRepository: UserPreferencesRepository,
    onIssueClick: (Int) -> Unit = {},
    onViewOnComicVine: () -> Unit = {},
    viewModel: ComicDetailViewModel = viewModel(
        factory = ComicDetailViewModel.factory(seriesId, database, prefsRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    val displayedIssues by viewModel.displayedIssues.collectAsStateWithLifecycle()
    val collectionIssueIds by viewModel.collectionIssueIds.collectAsStateWithLifecycle()
    val seriesCreators by viewModel.seriesCreators.collectAsStateWithLifecycle()
    val sortOrder by viewModel.issuesSortOrder.collectAsStateWithLifecycle()
    val collectionFilter by viewModel.issuesCollectionFilter.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        when (val state = uiState) {
            is ComicDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextPrimary)
                TopBackButton(onBack = onBack)
            }
            is ComicDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                TopBackButton(onBack = onBack)
            }
            is ComicDetailUiState.Success -> DetailContent(
                series = state.series,
                displayedIssues = displayedIssues,
                collectionIssueIds = collectionIssueIds,
                creators = seriesCreators,
                sortOrder = sortOrder,
                collectionFilter = collectionFilter,
                onToggleSort = { viewModel.toggleSortOrder() },
                onSetFilter = { viewModel.setCollectionFilter(it) },
                onBack = onBack,
                addState = addState,
                onAddToCollection = { viewModel.addToCollection() },
                onRemoveFromCollection = { viewModel.removeFromCollection() },
                onIssueClick = onIssueClick,
                onViewOnComicVine = onViewOnComicVine
            )
        }
    }
}

@Composable
private fun DetailContent(
    series: ComicVineVolume,
    displayedIssues: List<ComicVineIssueSummary>,
    collectionIssueIds: Set<Int>,
    creators: List<CreatorEntity>,
    sortOrder: IssuesSortOrder,
    collectionFilter: IssuesCollectionFilter,
    onToggleSort: () -> Unit,
    onSetFilter: (IssuesCollectionFilter) -> Unit,
    onBack: () -> Unit,
    addState: AddCollectionState,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    onIssueClick: (Int) -> Unit,
    onViewOnComicVine: () -> Unit
) {
    val coverUrl = series.image?.coverUrl()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── Hero image ────────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.45f to Color.Black.copy(alpha = 0.05f),
                            1.0f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
                TopBackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.width(88.dp).height(124.dp)
                    ) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = series.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.series_type_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(
                                1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                            )
                        )
                        Text(
                            text = series.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildHeroMeta(series.startYear, series.publisher?.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (series.countOfIssues != null && series.countOfIssues > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            IssueBadge(count = series.countOfIssues)
                        }
                    }
                }
            }
        }

        // ── Description ───────────────────────────────────────────────────
        val summary = series.description?.takeIf { it.isNotBlank() }?.stripHtml()
        summary?.let {
            item {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        // ── Creators ──────────────────────────────────────────────────────
        if (creators.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.background(ScreenBackground)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.creators_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    creators.forEach { creator ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = creator.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            creator.role?.takeIf { it.isNotBlank() }?.let { role ->
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Add to collection ─────────────────────────────────────────────
        item {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.background(ScreenBackground)
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val isLoading = addState == AddCollectionState.Checking ||
                        addState == AddCollectionState.Adding ||
                        addState == AddCollectionState.Removing
                if (addState == AddCollectionState.InCollection) {
                    Button(
                        onClick = onRemoveFromCollection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(Res.string.remove_from_collection)) }
                } else {
                    Button(
                        onClick = onAddToCollection,
                        enabled = addState == AddCollectionState.Idle,
                        colors = if (addState == AddCollectionState.Added) ButtonDefaults.buttonColors(
                            disabledContainerColor = BadgeGreen,
                            disabledContentColor = Color.White
                        ) else ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            when (addState) {
                                AddCollectionState.Checking,
                                AddCollectionState.Adding,
                                AddCollectionState.Removing -> stringResource(Res.string.loading)
                                AddCollectionState.Added -> stringResource(Res.string.added_to_collection)
                                else -> stringResource(Res.string.add_to_collection)
                            }
                        )
                    }
                }
            }
        }

        // ── View on ComicVine ─────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenBackground)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = onViewOnComicVine,
                    colors = ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.view_on_comicvine)) }
            }
        }

        // ── Issues header + sort/filter controls ──────────────────────────
        item {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.background(ScreenBackground)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenBackground)
                    .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.issues_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleSort) {
                    Icon(
                        imageVector = if (sortOrder == IssuesSortOrder.NUMBER_ASC)
                            Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (sortOrder == IssuesSortOrder.NUMBER_ASC)
                            stringResource(Res.string.sort_descending_cd) else stringResource(Res.string.sort_ascending_cd),
                        tint = TextSecondary
                    )
                }
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenBackground),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    IssueFilterChip(
                        text = stringResource(Res.string.filter_all),
                        selected = collectionFilter == IssuesCollectionFilter.ALL,
                        onClick = { onSetFilter(IssuesCollectionFilter.ALL) }
                    )
                }
                item {
                    IssueFilterChip(
                        text = stringResource(Res.string.filter_in_collection),
                        selected = collectionFilter == IssuesCollectionFilter.IN_COLLECTION,
                        onClick = { onSetFilter(IssuesCollectionFilter.IN_COLLECTION) }
                    )
                }
                item {
                    IssueFilterChip(
                        text = stringResource(Res.string.filter_not_in_collection),
                        selected = collectionFilter == IssuesCollectionFilter.NOT_IN_COLLECTION,
                        onClick = { onSetFilter(IssuesCollectionFilter.NOT_IN_COLLECTION) }
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(ScreenBackground)
            )
        }

        // ── Issues grid ───────────────────────────────────────────────────
        if (displayedIssues.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(ScreenBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_issues_match_filter),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            val rows = displayedIssues.chunked(3)
            items(rows) { rowIssues ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowIssues.forEach { issue ->
                        IssueGridCell(
                            issue = issue,
                            inCollection = issue.id in collectionIssueIds,
                            modifier = Modifier.weight(1f),
                            onClick = { onIssueClick(issue.id) }
                        )
                    }
                    repeat(3 - rowIssues.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(ScreenBackground)
            )
        }
    }
}

@Composable
private fun IssueFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.15f),
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun IssueGridCell(
    issue: ComicVineIssueSummary,
    inCollection: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(0.67f)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = issue.image?.coverUrl(),
                contentDescription = "#${issue.issueNumber}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Issue number at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f)),
                    RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${issue.issueNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        // IN COLLECTION banner at top
        if (inCollection) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        BadgeGreen.copy(alpha = 0.9f),
                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.in_collection_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IssueBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(BadgeGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = stringResource(Res.string.issues_count_label), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TopBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlatformBackButton(onBack = onBack, modifier = modifier, overlaid = true)
}

private fun buildHeroMeta(startYear: String?, publisher: String?): String =
    buildString {
        startYear?.let { append(it) }
        publisher?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }

private fun String.stripHtml(): String =
    replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

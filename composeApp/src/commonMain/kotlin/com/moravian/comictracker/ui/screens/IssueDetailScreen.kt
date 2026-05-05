package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import com.moravian.comictracker.network.ComicVineCharacter
import com.moravian.comictracker.network.ComicVineIssue
import com.moravian.comictracker.network.coverUrl
import com.moravian.comictracker.ui.components.PlatformBackButton
import com.moravian.comictracker.ui.viewmodels.AddCollectionState
import com.moravian.comictracker.ui.viewmodels.IssueDetailUiState
import com.moravian.comictracker.ui.viewmodels.IssueDetailViewModel
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.add_to_collection
import comictracker.composeapp.generated.resources.added_to_collection
import comictracker.composeapp.generated.resources.characters_label
import comictracker.composeapp.generated.resources.credits_label
import comictracker.composeapp.generated.resources.issue_type_label
import comictracker.composeapp.generated.resources.loading
import comictracker.composeapp.generated.resources.remove_from_collection
import comictracker.composeapp.generated.resources.view_on_comicvine
import org.jetbrains.compose.resources.stringResource

private val ScreenBackground = Color(0xFF0F0F0F)
private val IssueTextPrimary = Color.White
private val IssueTextSecondary = Color(0xFFAAAAAA)
private val IssueBadgeGreen = Color(0xFF2E7D32)

/** Detailed view for a single comic issue, including cover art, description, credits, and characters. */
@Composable
fun IssueDetailScreen(
    issueId: Int,
    onBack: () -> Unit,
    database: ComicTrackerDatabase,
    onViewOnComicVine: () -> Unit = {},
    viewModel: IssueDetailViewModel = viewModel(factory = IssueDetailViewModel.factory(issueId, database))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        when (val state = uiState) {
            is IssueDetailUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = IssueTextPrimary
                )
                IssueBackButton(onBack = onBack)
            }
            is IssueDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                IssueBackButton(onBack = onBack)
            }
            is IssueDetailUiState.Success -> IssueDetailContent(
                issue = state.issue,
                onBack = onBack,
                addState = addState,
                onAddToCollection = { viewModel.addToCollection() },
                onRemoveFromCollection = { viewModel.removeFromCollection() },
                onViewOnComicVine = onViewOnComicVine
            )
        }
    }
}

@Composable
private fun IssueDetailContent(
    issue: ComicVineIssue,
    onBack: () -> Unit,
    addState: AddCollectionState,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    onViewOnComicVine: () -> Unit
) {
    val imageUrl = issue.image?.coverUrl()
    val volumeName = "${issue.volume?.name ?: "Issue"} #${issue.issueNumber}"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── Hero image ────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Black.copy(alpha = 0.05f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                )
                IssueBackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
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
                            model = imageUrl,
                            contentDescription = volumeName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.issue_type_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = IssueTextSecondary,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(
                                1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                            )
                        )
                        Text(
                            text = volumeName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IssueTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildIssueMeta(null, issue.coverDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IssueTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IssueNumberBadge(issueNumber = issue.issueNumber)
                    }
                }
            }
        }

        // ── Description ───────────────────────────────────────────────────
        val summary = issue.description?.takeIf { it.isNotBlank() }?.stripIssueHtml()
        summary?.let {
            item {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IssueTextSecondary,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        // ── Credits ───────────────────────────────────────────────────────
        val credits = issue.personCredits.take(3)
        if (credits.isNotEmpty()) {
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
                        text = stringResource(Res.string.credits_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = IssueTextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    credits.forEach { credit ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = credit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IssueTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            credit.role?.takeIf { it.isNotBlank() }?.let { role ->
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IssueTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Characters ────────────────────────────────────────────────────
        if (issue.characterCredits.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.background(ScreenBackground)
                )
                Text(
                    text = stringResource(Res.string.characters_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = IssueTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(issue.characterCredits.take(20)) { character ->
                        IssueCharacterChip(character)
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(ScreenBackground)
                )
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
                val isLoading = addState == AddCollectionState.Checking || addState == AddCollectionState.Adding || addState == AddCollectionState.Removing
                if (addState == AddCollectionState.InCollection) {
                    Button(
                        onClick = onRemoveFromCollection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.remove_from_collection))
                    }
                } else {
                    Button(
                        onClick = onAddToCollection,
                        enabled = addState == AddCollectionState.Idle,
                        colors = if (addState == AddCollectionState.Added) ButtonDefaults.buttonColors(
                            disabledContainerColor = IssueBadgeGreen,
                            disabledContentColor = Color.White
                        ) else ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            when (addState) {
                                AddCollectionState.Checking, AddCollectionState.Adding, AddCollectionState.Removing -> stringResource(Res.string.loading)
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
                ) {
                    Text(stringResource(Res.string.view_on_comicvine))
                }
            }
        }
    }
}

@Composable
private fun IssueNumberBadge(issueNumber: String) {
    Box(
        modifier = Modifier
            .background(IssueBadgeGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "#$issueNumber",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.issue_type_label),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun IssueCharacterChip(character: ComicVineCharacter) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = character.name,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            labelColor = IssueTextPrimary
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color.White.copy(alpha = 0.15f)
        )
    )
}

@Composable
private fun IssueBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlatformBackButton(onBack = onBack, modifier = modifier, overlaid = true)
}

private fun buildIssueMeta(publisher: String?, coverDate: String?): String =
    buildString {
        publisher?.let { append(it) }
        coverDate?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }

private fun String.stripIssueHtml(): String =
    replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

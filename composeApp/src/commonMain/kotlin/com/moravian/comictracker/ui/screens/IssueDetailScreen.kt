package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.moravian.comictracker.network.ComicVineCharacterRef
import com.moravian.comictracker.network.ComicVineIssue
import com.moravian.comictracker.ui.viewmodels.IssueDetailUiState
import com.moravian.comictracker.ui.viewmodels.IssueDetailViewModel

private val ScreenBackground = Color(0xFF121212)
private val IssueTextPrimary = Color.White
private val IssueTextSecondary = Color(0xFFAAAAAA)
private val IssueBadgeGreen = Color(0xFF2E7D32)
private val IssueOverlayDark = Color.Black.copy(alpha = 0.55f)

@Composable
fun IssueDetailScreen(
    issueId: Int,
    onBack: () -> Unit,
    viewModel: IssueDetailViewModel = viewModel(factory = IssueDetailViewModel.factory(issueId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            is IssueDetailUiState.Success -> IssueDetailContent(issue = state.issue, onBack = onBack)
        }
    }
}

@Composable
private fun IssueDetailContent(issue: ComicVineIssue, onBack: () -> Unit) {
    val displayTitle = issue.name?.takeIf { it.isNotBlank() } ?: "Issue #${issue.issueNumber}"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                AsyncImage(
                    model = issue.image?.originalUrl ?: issue.image?.mediumUrl,
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
                            model = issue.image?.mediumUrl,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ISSUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = IssueTextSecondary,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(
                                1.5f, androidx.compose.ui.unit.TextUnitType.Sp
                            )
                        )
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IssueTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildIssueMeta(issue.volume?.name, issue.coverDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IssueTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IssueNumberBadge(issueNumber = issue.issueNumber)
                    }
                }
            }
        }

        val summary = issue.deck?.takeIf { it.isNotBlank() }
            ?: issue.description?.takeIf { it.isNotBlank() }?.stripIssueHtml()
        summary?.let {
            item {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IssueTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        if (issue.characters.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.background(ScreenBackground)
                )
                Text(
                    text = "Characters",
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(issue.characters.take(20)) { character ->
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
                Button(
                    onClick = { /* TODO: add to collection */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Collection")
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
                text = "ISSUE",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun IssueCharacterChip(character: ComicVineCharacterRef) {
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
    IconButton(
        onClick = onBack,
        modifier = modifier
            .padding(top = 12.dp, start = 12.dp)
            .background(IssueOverlayDark, CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = IssueTextPrimary
        )
    }
}

private fun buildIssueMeta(seriesName: String?, coverDate: String?): String =
    buildString {
        seriesName?.let { append(it) }
        coverDate?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }

private fun String.stripIssueHtml(): String =
    replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

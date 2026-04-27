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
import com.moravian.comictracker.network.ComicVineVolume
import com.moravian.comictracker.ui.viewmodels.ComicDetailUiState
import com.moravian.comictracker.ui.viewmodels.ComicDetailViewModel

private val ScreenBackground = Color(0xFF121212)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val BadgeGreen = Color(0xFF2E7D32)
private val OverlayDark = Color.Black.copy(alpha = 0.55f)

@Composable
fun ComicDetailScreen(
    volumeId: Int,
    onBack: () -> Unit,
    viewModel: ComicDetailViewModel = viewModel(factory = ComicDetailViewModel.factory(volumeId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        when (val state = uiState) {
            is ComicDetailUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TextPrimary
                )
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
            is ComicDetailUiState.Success -> DetailContent(volume = state.volume, onBack = onBack)
        }
    }
}

@Composable
private fun DetailContent(volume: ComicVineVolume, onBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                AsyncImage(
                    model = volume.image?.originalUrl ?: volume.image?.mediumUrl,
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
                            model = volume.image?.mediumUrl,
                            contentDescription = volume.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "VOLUME",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        Text(
                            text = volume.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildHeroMeta(volume.startYear, volume.publisher?.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (volume.issueCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            IssueBadge(count = volume.issueCount)
                        }
                    }
                }
            }
        }

        // ── Deck (plain-text summary) ─────────────────────────────────────
        val summary = volume.deck?.takeIf { it.isNotBlank() }
            ?: volume.description?.takeIf { it.isNotBlank() }?.stripHtml()
        summary?.let {
            item {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScreenBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        // ── Characters ────────────────────────────────────────────────────
        if (volume.characters.isNotEmpty()) {
            item {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.background(ScreenBackground)
                )
                Text(
                    text = "Characters",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
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
                    items(volume.characters.take(20)) { character ->
                        CharacterChip(character)
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

        // ── Divider + Action ──────────────────────────────────────────────
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
private fun IssueBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(BadgeGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ISSUES",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TopBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .padding(top = 12.dp, start = 12.dp)
            .background(OverlayDark, CircleShape)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary
        )
    }
}

@Composable
private fun CharacterChip(character: ComicVineCharacterRef) {
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
            labelColor = TextPrimary
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color.White.copy(alpha = 0.15f)
        )
    )
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

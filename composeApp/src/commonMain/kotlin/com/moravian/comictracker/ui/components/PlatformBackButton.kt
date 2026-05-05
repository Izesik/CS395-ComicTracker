package com.moravian.comictracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    overlaid: Boolean = false
)

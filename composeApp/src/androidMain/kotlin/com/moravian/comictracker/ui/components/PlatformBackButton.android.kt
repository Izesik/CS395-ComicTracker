package com.moravian.comictracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformBackButton(
    onBack: () -> Unit,
    modifier: Modifier,
    overlaid: Boolean
) {
    val iconModifier = if (overlaid) {
        modifier
            .padding(top = 12.dp, start = 12.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
    } else {
        modifier
    }
    IconButton(onClick = onBack, modifier = iconModifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White
        )
    }
}

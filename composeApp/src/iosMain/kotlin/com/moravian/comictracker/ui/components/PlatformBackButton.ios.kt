package com.moravian.comictracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.back_cd
import org.jetbrains.compose.resources.stringResource

/** iOS back button styled as a native chevron + "Back" text label. */
@Composable
actual fun PlatformBackButton(
    onBack: () -> Unit,
    modifier: Modifier,
    overlaid: Boolean
) {
    val buttonModifier = if (overlaid) {
        modifier
            .padding(top = 10.dp, start = 8.dp)
            .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
    } else {
        modifier
    }
    TextButton(
        onClick = onBack,
        modifier = buttonModifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = stringResource(Res.string.back_cd),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

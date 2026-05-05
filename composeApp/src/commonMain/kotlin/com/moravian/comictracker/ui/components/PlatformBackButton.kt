package com.moravian.comictracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific back button.
 *
 * On Android this renders a circular icon button; on iOS it uses the native back chevron style.
 *
 * @param onBack Called when the user taps the button.
 * @param modifier Optional modifier applied to the button container.
 * @param overlaid When true the button is rendered over content (e.g. a hero image) with a
 *   semi-transparent background; when false it is inline in the app bar.
 */
@Composable
expect fun PlatformBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    overlaid: Boolean = false
)

package com.moravian.comictracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific WebView composable.
 *
 * Loads and displays [url] using the native web rendering engine.
 * Android uses [android.webkit.WebView]; iOS uses WKWebView.
 *
 * @param url The URL to load.
 * @param modifier Modifier applied to the WebView container.
 */
@Composable
expect fun PlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
)

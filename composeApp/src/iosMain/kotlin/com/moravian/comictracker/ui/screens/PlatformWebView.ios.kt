package com.moravian.comictracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    UIKitView(
        factory = {
            WKWebView().apply {
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest(it)) }
            }
        },
        update = { webView ->
            NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest(it)) }
        },
        modifier = modifier
    )
}

package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moravian.comictracker.ui.components.PlatformBackButton

private val WebViewBackground = Color(0xFF121212)
private val WebViewTextPrimary = Color.White

@Composable
fun ComicWebViewScreen(url: String, title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(WebViewBackground)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(end = 16.dp)
        ) {
            PlatformBackButton(onBack = onBack, overlaid = false)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = WebViewTextPrimary,
                maxLines = 1
            )
        }
        PlatformWebView(url = url, modifier = Modifier.fillMaxSize())
    }
}

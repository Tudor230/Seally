package com.example.seally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AppScreenBackground(
    assetPath: String,
    modifier: Modifier = Modifier,
    overlayTransparency: Float = 0.3f,
) {
    val context = LocalContext.current
    val backgroundRequest = remember(context, assetPath) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$assetPath")
            .build()
    }
    val overlayAlpha = (1f - overlayTransparency).coerceIn(0f, 1f)
    val overlayColor = if (isSystemInDarkTheme()) Color.Black else Color(0xFFFFFCF7)

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = backgroundRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 1f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor.copy(alpha = overlayAlpha))
        )
    }
}

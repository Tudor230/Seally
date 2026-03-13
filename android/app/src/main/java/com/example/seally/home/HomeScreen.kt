package com.example.seally.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val settingsSvgRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/settings_icon.svg")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Home",
            modifier = Modifier.align(Alignment.Center),
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            AsyncImage(
                model = settingsSvgRequest,
                contentDescription = "Settings",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

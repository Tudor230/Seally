package com.example.seally.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
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
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val settingsSvgRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/settings_icon.png")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    val headerImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/line.png")
        .build()

    val skinnyImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/skinny - no background.png")
        .build()

    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Home",
            modifier = Modifier.align(Alignment.Center),
        )

        // Image placed under the top buttons (profile/settings)
        AsyncImage(
            model = headerImageRequest,
            contentDescription = "Header image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 95.dp)
                .size(220.dp),
        )

        // Main character image centered above the bottom navigation bar
        AsyncImage(
            model = skinnyImageRequest,
            contentDescription = "Character",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Push it up so it sits above the bottom nav bar
                .offset(y = (-92).dp)
                .size(400.dp),
        )

        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(30.dp),
            )
        }

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
                modifier = Modifier.size(60.dp),
            )
        }
    }
}

package com.example.seally.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

    // Image Requests
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

    // Temporary progress values (wire these to real state later)
    val caloriesProgress = 0.55f
    val waterProgress = 0.35f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // --- Top Navigation Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Centered level line between Profile and Settings
            AsyncImage(
                model = headerImageRequest,
                contentDescription = "Level Line",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(24.dp)
            )

            IconButton(onClick = onSettingsClick) {
                AsyncImage(
                    model = settingsSvgRequest,
                    contentDescription = "Settings",
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        // --- Main Content Area ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 90.dp, // Space for top bar.dp, // moved 50dp further below the top bar
                    bottom = 100.dp // Space for bottom nav
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Additional gap so the stat lines sit even lower
            Spacer(modifier = Modifier.height(12.dp))

            // Calories + Water (side-by-side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatLine(
                    icon = Icons.Default.Favorite,
                    label = "Calories",
                    progress = caloriesProgress,
                    modifier = Modifier.weight(1f)
                )

                StatLine(
                    icon = Icons.Default.AccountCircle,
                    label = "Water",
                    progress = waterProgress,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Character (bigger)
            AsyncImage(
                model = skinnyImageRequest,
                contentDescription = "Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun StatLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

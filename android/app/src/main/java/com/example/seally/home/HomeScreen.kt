package com.example.seally.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.profile.ProfileRoute
import com.example.seally.ui.components.LinearProgressBar
import com.example.seally.ui.components.PixelProgressBar
import com.example.seally.xp.XpViewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    var showProfile by rememberSaveable { mutableStateOf(false) }

    if (showProfile) {
        ProfileRoute(
            modifier = modifier,
            onBackClick = { showProfile = false },
        )
        return
    }

    val context = LocalContext.current

    val xpViewModel: XpViewModel = viewModel(factory = XpViewModel.Factory)
    val levelState by xpViewModel.levelState.collectAsState()

    // Image Requests
    val settingsSvgRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/settings_icon.png")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    val skinnyImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/muscles.png")
        .build()

    val profilePictureRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/profilePicture.png")
        .build()

    val homeBackgroundRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/page.png")
        .build()

    // Temporary progress values (wire these to real state later)
    val caloriesProgress = 0.55f
    val waterProgress = 0.35f


    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        AsyncImage(
            model = homeBackgroundRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // --- Top Navigation Row ---

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    showProfile = true
                    onProfileClick()
                },
            ) {
                AsyncImage(
                    model = profilePictureRequest,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }

            // Center XP/Level bar between Profile and Settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Lv ${levelState.level}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LinearProgressBar(
                        progress = levelState.progress,
                        modifier = Modifier.weight(1f),
                        height = 12.dp,
                        filledColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        cornerRadius = 6.dp,
                        borderWidth = 1.dp,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${levelState.xpIntoLevel}/${levelState.xpNeededForNextLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }

                // Temporary controls so you can see it increase/decrease
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { xpViewModel.addXp(-25) }) { Text("-XP") }
                    TextButton(onClick = { xpViewModel.addXp(25) }) { Text("+XP") }
                }
            }

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
                    top = 90.dp,
                    bottom = 100.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

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
                    icon = Icons.Default.Favorite,
                    label = "Water",
                    progress = waterProgress,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = skinnyImageRequest,
                contentDescription = "Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 12.dp)
            )
        }

        // Keep character anchoring consistent with ExercisesScreen.
        AsyncImage(
            model = skinnyImageRequest,
            contentDescription = "Character",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.88f)
                .padding(horizontal = 12.dp)
        )
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

        PixelProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            blocks = 14,
            blockWidth = 7.dp,
            blockHeight = 10.dp,
            gap = 2.dp,
            filledColor = MaterialTheme.colorScheme.primary,
            emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            cornerRadius = 2.dp,
        )
    }
}

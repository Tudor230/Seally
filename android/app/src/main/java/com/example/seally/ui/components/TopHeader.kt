package com.example.seally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.seally.xp.XpViewModel
import coil.request.ImageRequest

@Composable
fun TopHeader(
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val xpViewModel: XpViewModel = viewModel(factory = XpViewModel.Factory)
    val levelState by xpViewModel.levelState.collectAsState()
    val expectedLevelState by xpViewModel.expectedLevelState.collectAsState()
    val pendingTodayXp by xpViewModel.pendingTodayXp.collectAsState()

    val profilePictureRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/profilePicture.png")
        .build()

    val settingsImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/settings_icon.png")
        .build()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onProfileClick,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                AsyncImage(
                    model = profilePictureRequest,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Level ${levelState.level}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "XP ${levelState.xpIntoLevel}/${levelState.xpNeededForNextLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (pendingTodayXp != 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val sign = if (pendingTodayXp > 0) "+" else ""
                            Text(
                                text = "Expected ${sign}${pendingTodayXp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                LinearProgressBar(
                    progress = expectedLevelState.progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    filledColor = if (pendingTodayXp >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    borderColor = Color.Transparent,
                    cornerRadius = 3.dp,
                    borderWidth = 0.dp,
                )
            }

            Surface(
                onClick = onSettingsClick,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                AsyncImage(
                    model = settingsImageRequest,
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
fun LinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.background(borderColor, RoundedCornerShape(cornerRadius)).padding(borderWidth)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(filledColor)
        )
    }
}

package com.example.seally.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.profile.ProfileRoute
import com.example.seally.ui.components.TopHeader
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

    val musclesImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/muscles.png")
        .build()

    val backgroundRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/homepage.png")
        .build()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // --- Background Image with Transparency ---
        AsyncImage(
            model = backgroundRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.7f // Apply transparency effect
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TopHeader(
                onProfileClick = {
                    showProfile = true
                    onProfileClick()
                },
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // Character - consistent anchoring
        AsyncImage(
            model = musclesImageRequest,
            contentDescription = "Seal Character",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.75f)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun HomeStatCard(
    title: String,
    value: String,
    unit: String,
    progress: Float,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), // More solid but still a bit transparent
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            com.example.seally.ui.components.LinearProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(6.dp),
                filledColor = color,
                trackColor = color.copy(alpha = 0.1f),
                borderColor = Color.Transparent,
                cornerRadius = 3.dp,
                borderWidth = 0.dp
            )
        }
    }
}

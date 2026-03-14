package com.example.seally.exercises

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seally.calendar.CalendarScreen

@Composable
fun ExercisesScreen(
    modifier: Modifier = Modifier,
    onLeftActionClick: () -> Unit = {},
    onRightActionClick: () -> Unit = {},
) {
    var showDumbbellPage by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }

    if (showDumbbellPage) {
        DumbbellWorkoutsScreen(
            modifier = modifier,
            onBackClick = { showDumbbellPage = false },
        )
        return
    }

    if (showCalendar) {
        CalendarScreen(
            modifier = modifier,
            onBackClick = { showCalendar = false },
        )
        return
    }

    val context = LocalContext.current

    val headerImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/line.png")
        .build()

    val skinnyImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/lilseal.png")
        .build()

    val calendarIconRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/sealcalendar.png")
        .build()

    val dumbbellIconRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/dumbbell_icon - no background.png")
        .build()

    val backgroundRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/background_exercises.png")
        .build()

    Box(modifier = modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = backgroundRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.65f,
            modifier = Modifier.fillMaxSize(),
        )

        // Top header image (same style as Home)
        AsyncImage(
            model = headerImageRequest,
            contentDescription = "Header image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
                .size(220.dp),
        )

        // Seal/character in the middle, above the bottom nav
        AsyncImage(
            model = skinnyImageRequest,
            contentDescription = "Seal",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp)
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp)
        )

        // Two action buttons bottom-left / bottom-right of the seal, above the nav bar
        IconButton(
            onClick = {
                onLeftActionClick()
                showDumbbellPage = true
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 60.dp)
                .semantics { contentDescription = "Dumbbell" },
        ) {
            AsyncImage(
                model = dumbbellIconRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp),
            )
        }

        IconButton(
            onClick = {
                onRightActionClick()
                showCalendar = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 60.dp)
                .semantics { contentDescription = "Calendar" },
        ) {
            AsyncImage(
                model = calendarIconRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

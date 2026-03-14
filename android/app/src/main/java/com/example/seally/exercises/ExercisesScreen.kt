package com.example.seally.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seally.calendar.CalendarScreen
import com.example.seally.camera.CameraScreen
import com.example.seally.camera.CameraViewModel
import com.example.seally.camera.ExerciseType
import com.example.seally.ui.components.TopHeader

@Composable
fun ExercisesScreen(
    modifier: Modifier = Modifier,
    mCameraViewModel: CameraViewModel,
    onLeftActionClick: () -> Unit = {},
    onRightActionClick: () -> Unit = {},
) {
    var showDumbbellPage by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var mSelectedExerciseForChecker by remember { mutableStateOf<ExerciseType?>(null) }

    if (mSelectedExerciseForChecker != null) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraScreen(
                modifier = Modifier.fillMaxSize(),
                mViewModel = mCameraViewModel,
                mShowExerciseGuideOnEntry = true,
            )
            IconButton(
                onClick = { mSelectedExerciseForChecker = null },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        return
    }

    if (showDumbbellPage) {
        DumbbellWorkoutsScreen(
            modifier = modifier,
            onBackClick = { showDumbbellPage = false },
            onExerciseSelected = { mExercise ->
                mCameraViewModel.setSelectedExercise(mExercise)
                mSelectedExerciseForChecker = mExercise
            },
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
            alpha = 0.7f
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeader()

            // Main content empty for now to show character, or we can add some subtle stats
            Box(modifier = Modifier.weight(1f))
        }

        // --- Action Buttons (Similar to Nutrition Section) ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FloatingActionButton(
                onClick = {
                    onLeftActionClick()
                    showDumbbellPage = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts", modifier = Modifier.size(32.dp))
            }

            FloatingActionButton(
                onClick = {
                    onRightActionClick()
                    showCalendar = true
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "History", modifier = Modifier.size(32.dp))
            }
        }

        // Seal/character - consistent anchoring
        AsyncImage(
            model = musclesImageRequest,
            contentDescription = "Seal",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.75f)
                .padding(bottom = 20.dp)
        )
    }
}

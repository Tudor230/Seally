package com.example.seally

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.camera.CameraViewModel
import com.example.seally.exercises.ExercisesScreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.seally.goals.GoalsScreen
import com.example.seally.home.HomeScreen
import com.example.seally.onboarding.OnboardingScreen
import com.example.seally.nutrition.NutritionScreen
import com.example.seally.nutrition.NutritionViewModel
import com.example.seally.profile.ProfileViewModel
import com.example.seally.profile.ProfileRoute
import com.example.seally.ui.theme.SeallyTheme

private val mBottomNavIconSize = 28.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = AndroidColor.TRANSPARENT
        window.statusBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            SeallyTheme {
                val cameraViewModel: CameraViewModel = viewModel()
                val uiState by cameraViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
                val profile by profileViewModel.profile.collectAsState()

                LaunchedEffect(uiState.mFormFeedback.mProblematicJoints, uiState.mFormFeedback.mErrorMessage) {
                    val resultIntent = Intent().apply {
                        putStringArrayListExtra(
                            EXTRA_PROBLEMATIC_JOINTS,
                            ArrayList(uiState.mFormFeedback.mProblematicJoints),
                        )
                        putExtra(EXTRA_ERROR_MESSAGE, uiState.mFormFeedback.mErrorMessage)
                    }
                    setResult(RESULT_OK, resultIntent)
                }

                when (val p = profile) {
                    null -> {
                        // Loading state - show a clean loading screen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF00E5FF))
                        }
                    }
                    else -> {
                        if (p.onboardingCompleted) {
                            SeallyApp(mCameraViewModel = cameraViewModel)
                        } else {
                            OnboardingScreen()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PROBLEMATIC_JOINTS = "extra_problematic_joints"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }
}

@PreviewScreenSizes
@Composable
fun SeallyApp( mCameraViewModel: CameraViewModel = viewModel()) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val context = LocalContext.current
    val nutritionViewModel: NutritionViewModel = viewModel()

    LaunchedEffect(Unit) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            mCameraViewModel.preWarmPoseLandmarker()
        }
    }
    var shouldShowBottomBarForNutrition by rememberSaveable { mutableStateOf(true) }
    var shouldShowBottomBarForExercises by rememberSaveable { mutableStateOf(true) }
    var shouldShowProfile by rememberSaveable { mutableStateOf(false) }
    var profileStartingDestination by rememberSaveable { mutableStateOf(com.example.seally.profile.ProfileDestination.PROFILE) }
    val shouldShowSealCelebrationOverlay = nutritionViewModel.mShouldShowSealCelebration
    var lastBackPressTimestamp by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (shouldShowProfile) {
            shouldShowProfile = false
            return@BackHandler
        }

        if (currentDestination == AppDestinations.NUTRITION && nutritionViewModel.canNavigateBackInNutrition()) {
            nutritionViewModel.navigateBackInNutrition()
            return@BackHandler
        }

        if (currentDestination != AppDestinations.HOME) {
            currentDestination = AppDestinations.HOME
            return@BackHandler
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastBackPressTimestamp <= 1000L) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPressTimestamp = now
            Toast
                .makeText(context, "press back one more time to quit", Toast.LENGTH_SHORT)
                .show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                val shouldRenderBottomBar = !shouldShowProfile && when (currentDestination) {
                    AppDestinations.NUTRITION -> shouldShowBottomBarForNutrition
                    AppDestinations.EXERCISES -> shouldShowBottomBarForExercises
                    else -> true
                }
                if (shouldRenderBottomBar) {
                    NavigationBar {
                        AppDestinations.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = destination == currentDestination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    DestinationIcon(destination)
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val mLayoutDirection = LocalLayoutDirection.current
            val mShouldApplyBottomPadding = !shouldShowProfile && when (currentDestination) {
                AppDestinations.NUTRITION -> shouldShowBottomBarForNutrition
                AppDestinations.EXERCISES -> shouldShowBottomBarForExercises
                else -> true
            }
            val mContentModifier = Modifier.padding(
                start = innerPadding.calculateStartPadding(mLayoutDirection),
                end = innerPadding.calculateEndPadding(mLayoutDirection),
                bottom = if (mShouldApplyBottomPadding) innerPadding.calculateBottomPadding() else 0.dp,
            )

            if (shouldShowProfile) {
                ProfileRoute(
                    modifier = mContentModifier.fillMaxSize(),
                    initialDestination = profileStartingDestination,
                    onBackClick = { shouldShowProfile = false },
                )
            } else {
                when (currentDestination) {
                    AppDestinations.NUTRITION -> NutritionScreen(
                        modifier = mContentModifier,
                        onDetailVisibilityChanged = { shouldShowBottomBarForNutrition = it },
                        onProfileClick = { 
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.PROFILE
                            shouldShowProfile = true 
                        },
                        onSettingsClick = {
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.SETTINGS
                            shouldShowProfile = true
                        },
                        mViewModel = nutritionViewModel,
                    )
                    AppDestinations.GOALS -> GoalsScreen(
                        modifier = mContentModifier,
                        onProfileClick = { 
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.PROFILE
                            shouldShowProfile = true 
                        },
                        onSettingsClick = {
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.SETTINGS
                            shouldShowProfile = true
                        },
                    )
                    AppDestinations.HOME -> HomeScreen(
                        modifier = mContentModifier,
                        onProfileClick = { 
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.PROFILE
                            shouldShowProfile = true 
                        },
                        onSettingsClick = {
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.SETTINGS
                            shouldShowProfile = true
                        }
                    )
                    AppDestinations.EXERCISES -> ExercisesScreen(
                        modifier = mContentModifier,
                        mCameraViewModel = mCameraViewModel,
                        onDetailVisibilityChanged = { shouldShowBottomBarForExercises = it },
                        onProfileClick = { 
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.PROFILE
                            shouldShowProfile = true 
                        },
                        onSettingsClick = {
                            profileStartingDestination = com.example.seally.profile.ProfileDestination.SETTINGS
                            shouldShowProfile = true
                        },
                    )
                }
            }
        }

        if (shouldShowSealCelebrationOverlay) {
            FullScreenSealCelebrationOverlay()
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title)
    }
}

@Composable
private fun DestinationIcon(destination: AppDestinations) {
    val mAssetPath = when (destination) {
        AppDestinations.NUTRITION -> "icons/seal1.png"
        AppDestinations.GOALS -> "icons/seal2.png"
        AppDestinations.HOME -> "icons/seal3.png"
        AppDestinations.EXERCISES -> "icons/seal4.png"
        else -> null
    }

    if (mAssetPath != null) {
        val context = LocalContext.current
        val mSealBitmap = remember {
            context.assets.open(mAssetPath).use { mInputStream ->
                BitmapFactory.decodeStream(mInputStream).asImageBitmap()
            }
        }
        Image(
            bitmap = mSealBitmap,
            contentDescription = destination.label,
            modifier = Modifier.size(mBottomNavIconSize),
        )
    } else {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
        )
    }
}

@Composable
private fun FullScreenSealCelebrationOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "🦭",
            style = MaterialTheme.typography.displayLarge,
        )
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    EXERCISES("Exercises", Icons.Default.Home),
    NUTRITION("Nutrition", Icons.Default.Favorite),
    GOALS("Goals", Icons.Default.Home),
}

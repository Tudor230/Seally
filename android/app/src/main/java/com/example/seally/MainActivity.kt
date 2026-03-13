package com.example.seally

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.camera.CameraViewModel
import com.example.seally.home.HomeScreen
import com.example.seally.ui.theme.SeallyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeallyTheme {
                SeallyApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun SeallyApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.EXERCISES) }
    val context = LocalContext.current
    val cameraViewModel: CameraViewModel = viewModel()

    LaunchedEffect(Unit) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            cameraViewModel.preWarmPoseLandmarker()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentDestination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (currentDestination) {
            AppDestinations.NUTRITION -> PlaceholderScreen(
                title = "Nutrition",
                modifier = Modifier.padding(innerPadding),
            )
            AppDestinations.GOALS -> PlaceholderScreen(
                title = "Goals",
                modifier = Modifier.padding(innerPadding),
            )
            AppDestinations.HOME -> HomeScreen(
                modifier = Modifier.padding(innerPadding),
            )
            AppDestinations.EXERCISES -> PlaceholderScreen(
                title = "Exercises",
                modifier = Modifier.padding(innerPadding),
            )
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

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    NUTRITION("Nutrition", Icons.Default.Favorite),
    GOALS("Goals", Icons.Default.Home),
    HOME("Home", Icons.Default.Home),
    EXERCISES("Exercises", Icons.Default.Home),
}

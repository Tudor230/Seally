package com.example.seally.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class ProfileDestination {
    PROFILE,
    SETTINGS
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    initialDestination: ProfileDestination = ProfileDestination.PROFILE,
    onBackClick: () -> Unit,
) {
    var currentDestination by remember { mutableStateOf(initialDestination) }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState == ProfileDestination.SETTINGS) {
                slideInHorizontally { it } with slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } with slideOutHorizontally { it }
            }
        },
        label = "ProfileNavigation"
    ) { destination ->
        when (destination) {
            ProfileDestination.PROFILE -> {
                ProfileScreen(
                    modifier = modifier,
                    onBackClick = onBackClick,
                )
            }
            ProfileDestination.SETTINGS -> {
                SettingsScreen(
                    modifier = modifier,
                    onBackClick = { 
                        if (initialDestination == ProfileDestination.SETTINGS) {
                            onBackClick() // If we started at settings, back goes to Home
                        } else {
                            currentDestination = ProfileDestination.PROFILE 
                        }
                    }
                )
            }
        }
    }
}

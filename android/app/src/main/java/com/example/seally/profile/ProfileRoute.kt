package com.example.seally.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProfileRoute(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    ProfileScreen(
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

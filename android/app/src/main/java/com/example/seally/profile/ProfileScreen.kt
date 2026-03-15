package com.example.seally.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
    val profile by vm.profile.collectAsState()

    val profilePictureRequest = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/icons/profilePicture.png")
            .build()
    }

    // --- Visual tokens ---
    val fieldShape = RoundedCornerShape(12.dp)
    val cardShape = RoundedCornerShape(24.dp)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val backgroundColor = MaterialTheme.colorScheme.background

    @Composable
    fun friendlyOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    )

    // State Management
    var name by remember { mutableStateOf("") }
    var heightCmText by remember { mutableStateOf("") }
    var weightKgText by remember { mutableStateOf("") }
    var goalWeightKgText by remember { mutableStateOf("") }

    // Sync state with Profile data
    LaunchedEffect(profile) {
        profile?.let { currentProfile ->
            name = currentProfile.name
            heightCmText = currentProfile.heightCm?.toString().orEmpty()
            weightKgText = currentProfile.weightKg?.let { stripTrailingZeros(it) }.orEmpty()
            goalWeightKgText = currentProfile.goalWeightKg?.let { stripTrailingZeros(it) }.orEmpty()
        }
    }

    val currentProfile = profile ?: UserProfile()

    // Validation Logic
    val heightCm = heightCmText.toIntOrNull()
    val weightKg = weightKgText.toFloatOrNull()
    val goalWeightKg = goalWeightKgText.toFloatOrNull()

    val heightError = heightCmText.isNotBlank() && (heightCm == null || heightCm !in 50..260)
    val weightError = weightKgText.isNotBlank() && (weightKg == null || weightKg !in 20f..400f)
    val goalWeightError = goalWeightKgText.isNotBlank() && (goalWeightKg == null || goalWeightKg !in 20f..400f)
    val canSave = !heightError && !weightError && !goalWeightError && name.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Character stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    AsyncImage(
                        model = profilePictureRequest,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                color = container,
                border = BorderStroke(1.dp, outline),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Basic info",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g., Ioana") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = fieldShape,
                        colors = friendlyOutlinedTextFieldColors(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = heightCmText,
                            onValueChange = { heightCmText = it.filterNumeric(3) },
                            label = { Text("Height") },
                            placeholder = { Text("cm") },
                            modifier = Modifier.weight(1f),
                            isError = heightError,
                            shape = fieldShape,
                            colors = friendlyOutlinedTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (heightError) {
                                    Text("50–260 cm", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )

                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it.filterDecimal(6) },
                            label = { Text("Weight") },
                            placeholder = { Text("kg") },
                            modifier = Modifier.weight(1f),
                            isError = weightError,
                            shape = fieldShape,
                            colors = friendlyOutlinedTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = {
                                if (weightError) {
                                    Text("20–400 kg", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }

                    OutlinedTextField(
                        value = goalWeightKgText,
                        onValueChange = { goalWeightKgText = it.filterDecimal(6) },
                        label = { Text("Goal weight") },
                        placeholder = { Text("kg") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = goalWeightError,
                        shape = fieldShape,
                        colors = friendlyOutlinedTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            if (goalWeightError) {
                                Text("20–400 kg", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    vm.save(
                        UserProfile(
                            name = name.trim(),
                            heightCm = heightCm,
                            weightKg = weightKg,
                            goalWeightKg = goalWeightKg,
                            activityType = currentProfile.activityType,
                            workoutDaysPerWeek = currentProfile.workoutDaysPerWeek,
                            journeyGoal = currentProfile.journeyGoal,
                            waterTargetMl = currentProfile.waterTargetMl,
                            onboardingCompleted = currentProfile.onboardingCompleted,
                        )
                    )
                    onBackClick()
                },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "SAVE CHANGES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun String.filterNumeric(maxLen: Int): String {
    val filtered = filter { it.isDigit() }
    return if (filtered.length <= maxLen) filtered else filtered.take(maxLen)
}

private fun String.filterDecimal(maxLen: Int): String {
    val filtered = buildString {
        var dotUsed = false
        for (c in this@filterDecimal) {
            if (c.isDigit()) append(c)
            else if (c == '.' && !dotUsed) {
                dotUsed = true
                append(c)
            }
        }
    }
    return if (filtered.length <= maxLen) filtered else filtered.take(maxLen)
}

private fun stripTrailingZeros(value: Float): String {
    val s = value.toString()
    return if (s.contains(".")) s.trimEnd('0').trimEnd('.') else s
}

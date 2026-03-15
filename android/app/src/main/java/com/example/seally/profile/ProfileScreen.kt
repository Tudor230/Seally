package com.example.seally.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
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
    val backgroundRequest = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/backgrounds/form_validator.png")
            .build()
    }

    // --- Visual tokens (slightly rounded but still "pixel/card" friendly) ---
    val fieldShape = RoundedCornerShape(10.dp)
    val cardShape = RoundedCornerShape(14.dp)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)

    @Composable
    fun friendlyOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        errorContainerColor = container,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // State Management
    var name by remember { mutableStateOf("") }
    var heightCmText by remember { mutableStateOf("") }
    var weightKgText by remember { mutableStateOf("") }
    var goalWeightKgText by remember { mutableStateOf("") }

    // Sync state with Profile data
    LaunchedEffect(profile) {
        name = profile.name
        heightCmText = profile.heightCm?.toString().orEmpty()
        weightKgText = profile.weightKg?.let { stripTrailingZeros(it) }.orEmpty()
        goalWeightKgText = profile.goalWeightKg?.let { stripTrailingZeros(it) }.orEmpty()
    }

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
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Character stats",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "Set up once, tweak anytime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        AsyncImage(
                            model = profilePictureRequest,
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(44.dp)
                                .clip(CircleShape)

                        )
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = outline)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AsyncImage(
                model = backgroundRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.72f,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = container),
                    border = BorderStroke(1.dp, outline),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Basic info",
                            style = MaterialTheme.typography.titleSmall,
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
                                    Text(
                                        text = if (heightError) "50–260 cm" else "",
                                        color = if (heightError) MaterialTheme.colorScheme.error else Color.Transparent,
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = weightKgText,
                                onValueChange = { weightKgText = it.filterDecimal(6) },
                                label = { Text("Current weight") },
                                placeholder = { Text("kg") },
                                modifier = Modifier.weight(1f),
                                isError = weightError,
                                shape = fieldShape,
                                colors = friendlyOutlinedTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = {
                                    Text(
                                        text = if (weightError) "20–400 kg" else "",
                                        color = if (weightError) MaterialTheme.colorScheme.error else Color.Transparent,
                                    )
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
                            )
                        )
                        onBackClick()
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// --- Helper Functions (Ensuring these match your existing logic) ---

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

package com.example.seally.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.delay

private enum class NutritionPage {
    Kitchen,
    Food,
    Water,
    Camera,
}

private enum class MealType(val label: String) {
    Breakfast("Breakfast"),
    Lunch("Lunch"),
    Snack("Snack"),
    Dinner("Dinner"),
}

private data class FoodEntry(
    val name: String,
    val meal: MealType,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val isHealthy: Boolean,
)

@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
) {
    val calorieTarget = 2200
    val waterTargetMl = 2500
    val proteinTarget = 140
    val carbsTarget = 220
    val fatTarget = 70

    var currentPage by rememberSaveable { mutableStateOf(NutritionPage.Kitchen) }
    var waterConsumedMl by rememberSaveable { mutableIntStateOf(0) }
    var sealCelebrationTick by rememberSaveable { mutableIntStateOf(0) }
    var shouldShowSealCelebration by rememberSaveable { mutableStateOf(false) }

    val foods = remember {
        mutableStateListOf<FoodEntry>()
    }

    val caloriesConsumed = foods.sumOf { it.calories }
    val proteinConsumed = foods.sumOf { it.protein }
    val carbsConsumed = foods.sumOf { it.carbs }
    val fatsConsumed = foods.sumOf { it.fats }

    LaunchedEffect(sealCelebrationTick) {
        if (sealCelebrationTick == 0) {
            return@LaunchedEffect
        }
        shouldShowSealCelebration = true
        delay(2000)
        shouldShowSealCelebration = false
    }

    DisposableEffect(currentPage) {
        onDetailVisibilityChanged(currentPage == NutritionPage.Kitchen)
        onDispose {}
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentPage) {
            NutritionPage.Kitchen -> KitchenMainPage(
                caloriesConsumed = caloriesConsumed,
                calorieTarget = calorieTarget,
                waterConsumedMl = waterConsumedMl,
                waterTargetMl = waterTargetMl,
                onOpenFood = { currentPage = NutritionPage.Food },
                onOpenWater = { currentPage = NutritionPage.Water },
            )
            NutritionPage.Food -> FoodTrackingPage(
                foods = foods,
                caloriesConsumed = caloriesConsumed,
                calorieTarget = calorieTarget,
                proteinConsumed = proteinConsumed,
                proteinTarget = proteinTarget,
                carbsConsumed = carbsConsumed,
                carbsTarget = carbsTarget,
                fatsConsumed = fatsConsumed,
                fatTarget = fatTarget,
                onBack = { currentPage = NutritionPage.Kitchen },
                onOpenCamera = {
                    currentPage = NutritionPage.Camera
                },
                onManualAddFood = { addedFood ->
                    foods.add(addedFood)
                    sealCelebrationTick++
                },
            )
            NutritionPage.Water -> WaterTrackingPage(
                waterConsumedMl = waterConsumedMl,
                waterTargetMl = waterTargetMl,
                onBack = { currentPage = NutritionPage.Kitchen },
                onAddWater = { addedAmount -> waterConsumedMl += addedAmount },
            )
            NutritionPage.Camera -> CameraTrackingPage(
                onBack = { currentPage = NutritionPage.Food },
                onAddFoodFromScan = { scannedFood ->
                    foods.add(scannedFood)
                    sealCelebrationTick++
                    currentPage = NutritionPage.Food
                },
            )
        }
        if (shouldShowSealCelebration) {
            SealCelebrationOverlay()
        }
    }
}

@Composable
private fun KitchenMainPage(
    caloriesConsumed: Int,
    calorieTarget: Int,
    waterConsumedMl: Int,
    waterTargetMl: Int,
    onOpenFood: () -> Unit,
    onOpenWater: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TopMetric(
                label = "Calories",
                value = "$caloriesConsumed / $calorieTarget kcal",
                progress = (caloriesConsumed.toFloat() / calorieTarget).coerceIn(0f, 1f),
                reverse = false,
            )
            TopMetric(
                label = "Water",
                value = "$waterConsumedMl / $waterTargetMl ml",
                progress = (waterConsumedMl.toFloat() / waterTargetMl).coerceIn(0f, 1f),
                reverse = true,
            )
        }

        PlaceholderImage(
            modifier = Modifier.align(Alignment.Center),
        )

        IconOnlyActionButton(
            onClick = onOpenFood,
            iconGlyph = "🍽️",
            modifier = Modifier.align(Alignment.BottomStart),
        )

        IconOnlyActionButton(
            onClick = onOpenWater,
            iconGlyph = "💧",
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun FoodTrackingPage(
    foods: List<FoodEntry>,
    caloriesConsumed: Int,
    calorieTarget: Int,
    proteinConsumed: Int,
    proteinTarget: Int,
    carbsConsumed: Int,
    carbsTarget: Int,
    fatsConsumed: Int,
    fatTarget: Int,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onManualAddFood: (FoodEntry) -> Unit,
) {
    var shouldShowAddFoodDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = "Food Tracking",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            NutritionPanel(title = "Calories & Macros") {
                Text("$caloriesConsumed / $calorieTarget kcal")
                Spacer(modifier = Modifier.height(8.dp))
                ReversibleTracker(
                    progress = (caloriesConsumed.toFloat() / calorieTarget).coerceIn(0f, 1f),
                    reverse = false,
                    modifier = Modifier.width(220.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                MacroLine("Protein", proteinConsumed, proteinTarget)
                MacroLine("Carbs", carbsConsumed, carbsTarget)
                MacroLine("Fats", fatsConsumed, fatTarget)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Consumed foods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            MealType.entries.forEach { mealType ->
                val mealFoods = foods.filter { it.meal == mealType }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = mealType.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (mealFoods.isEmpty()) {
                            Text(
                                text = "No entries yet",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            mealFoods.forEach { food ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(food.name, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${food.calories} kcal")
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            IconOnlyActionButton(
                onClick = onOpenCamera,
                iconGlyph = "📷",
            )
            IconOnlyActionButton(
                onClick = { shouldShowAddFoodDialog = true },
                iconGlyph = "✍️",
            )
        }
    }

    if (shouldShowAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { shouldShowAddFoodDialog = false },
            onAddFood = { food ->
                onManualAddFood(food)
                shouldShowAddFoodDialog = false
            },
        )
    }
}

@Composable
private fun WaterTrackingPage(
    waterConsumedMl: Int,
    waterTargetMl: Int,
    onBack: () -> Unit,
    onAddWater: (Int) -> Unit,
) {
    val quantities = listOf(150, 250, 500)
    var selectedQuantity by rememberSaveable { mutableIntStateOf(250) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = "Water Tracking",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        PlaceholderImage(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(210.dp),
        )

        Spacer(modifier = Modifier.height(14.dp))

        NutritionPanel(title = "Water quantity") {
            Text("$waterConsumedMl / $waterTargetMl ml")
            Spacer(modifier = Modifier.height(8.dp))
            ReversibleTracker(
                progress = (waterConsumedMl.toFloat() / waterTargetMl).coerceIn(0f, 1f),
                reverse = false,
                modifier = Modifier.width(220.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            )
            {
                quantities.forEach { quantity ->
                    FilterChip(
                        selected = selectedQuantity == quantity,
                        onClick = { selectedQuantity = quantity },
                        label = { Text("$quantity ml") },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            IconOnlyActionButton(
                onClick = { onAddWater(selectedQuantity) },
                iconGlyph = "💧",
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CameraTrackingPage(
    onBack: () -> Unit,
    onAddFoodFromScan: (FoodEntry) -> Unit,
) {
    var mScannedResult by remember { mutableStateOf<NutritionLabelScanResult?>(null) }

    NutritionLabelScannerPage(
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        onScanResult = { scanResult -> mScannedResult = scanResult },
    )

    mScannedResult?.let { scannedResult ->
        AddScannedFoodDialog(
            suggestion = scannedResult,
            onDismiss = { mScannedResult = null },
            onAddFood = { newFood ->
                onAddFoodFromScan(newFood)
                mScannedResult = null
            },
        )
    }
}

@Composable
private fun MacroLine(
    label: String,
    value: Int,
    target: Int,
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text("$label: $value / $target g")
        ReversibleTracker(
            progress = (value.toFloat() / target).coerceIn(0f, 1f),
            reverse = false,
            modifier = Modifier.width(220.dp),
        )
    }
}

@Composable
private fun TopMetric(
    label: String,
    value: String,
    progress: Float,
    reverse: Boolean,
) {
    Column(
        horizontalAlignment = if (reverse) Alignment.End else Alignment.Start,
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        ReversibleTracker(progress = progress, reverse = reverse)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            textAlign = if (reverse) TextAlign.End else TextAlign.Start,
            modifier = Modifier.width(145.dp),
        )
    }
}

@Composable
private fun ReversibleTracker(
    progress: Float,
    reverse: Boolean,
    modifier: Modifier = Modifier.width(145.dp),
) {
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(99.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(99.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(if (reverse) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxWidth(progress)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun PlaceholderImage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PLACEHOLDER IMAGE",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun IconOnlyActionButton(
    onClick: () -> Unit,
    iconGlyph: String,
    modifier: Modifier = Modifier,
) {
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor = if (isDarkMode) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isDarkMode) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Button(
        onClick = onClick,
        modifier = modifier.size(68.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = iconGlyph,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NutritionPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AddFoodDialog(
    onDismiss: () -> Unit,
    onAddFood: (FoodEntry) -> Unit,
) {
    var selectedMeal by rememberSaveable { mutableStateOf(MealType.Breakfast) }
    var name by rememberSaveable { mutableStateOf("") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var fatsText by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add food") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Food name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                MealTypeSelector(
                    selectedMeal = selectedMeal,
                    onMealSelected = { selectedMeal = it },
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Calories") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Protein (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fatsText,
                    onValueChange = { fatsText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Fats (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val calories = caloriesText.toIntOrNull() ?: 0
                    val protein = proteinText.toIntOrNull() ?: 0
                    val carbs = carbsText.toIntOrNull() ?: 0
                    val fats = fatsText.toIntOrNull() ?: 0
                    val food = FoodEntry(
                        name = name.ifBlank { "Unnamed food" },
                        meal = selectedMeal,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fats = fats,
                        isHealthy = calories in 1..500,
                    )
                    onAddFood(food)
                },
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddScannedFoodDialog(
    suggestion: NutritionLabelScanResult,
    onDismiss: () -> Unit,
    onAddFood: (FoodEntry) -> Unit,
) {
    var selectedMeal by rememberSaveable(suggestion.recognizedText) { mutableStateOf(MealType.Lunch) }
    var name by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.name) }
    var caloriesText by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.calories.toString()) }
    var proteinText by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.protein.toString()) }
    var carbsText by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.carbs.toString()) }
    var fatsText by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.fats.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add scanned food") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Food name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                MealTypeSelector(
                    selectedMeal = selectedMeal,
                    onMealSelected = { selectedMeal = it },
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Calories") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Protein (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Carbs (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fatsText,
                    onValueChange = { fatsText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Fats (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val calories = caloriesText.toIntOrNull() ?: 0
                    val protein = proteinText.toIntOrNull() ?: 0
                    val carbs = carbsText.toIntOrNull() ?: 0
                    val fats = fatsText.toIntOrNull() ?: 0
                    val food = FoodEntry(
                        name = name.ifBlank { "Scanned food" },
                        meal = selectedMeal,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fats = fats,
                        isHealthy = calories in 1..500,
                    )
                    onAddFood(food)
                },
            ) {
                Text("Add scanned food")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MealTypeSelector(
    selectedMeal: MealType,
    onMealSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Meal",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MealType.entries.size) { index ->
                val meal = MealType.entries[index]
                FilterChip(
                    selected = selectedMeal == meal,
                    onClick = { onMealSelected(meal) },
                    label = { Text(meal.label) },
                )
            }
        }
    }
}

@Composable
private fun SealCelebrationOverlay() {
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

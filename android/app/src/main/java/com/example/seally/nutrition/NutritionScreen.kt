package com.example.seally.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class NutritionPage {
    Kitchen,
    Food,
    Water,
    Camera,
}

private enum class MealType(val label: String, val icon: String) {
    Breakfast("Breakfast", "🍳"),
    Lunch("Lunch", "🍱"),
    Snack("Snack", "🍎"),
    Dinner("Dinner", "🍛"),
}

private enum class ScannedQuantityOption(val label: String) {
    Quarter("1/4"),
    Half("1/2"),
    Full("Full"),
    Multiple("Multiple"),
    Grams("Grams"),
}

private data class ScannedQuantity(
    val multiplier: Float,
    val isValid: Boolean,
    val usesPerServingValues: Boolean,
    val summary: String,
)

private data class FoodEntry(
    val name: String,
    val meal: MealType,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val sugars: Int,
    val fibers: Int,
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
    val sugarTarget = 50
    val fiberTarget = 30

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
    val sugarsConsumed = foods.sumOf { it.sugars }
    val fibersConsumed = foods.sumOf { it.fibers }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
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
                sugarsConsumed = sugarsConsumed,
                sugarTarget = sugarTarget,
                fibersConsumed = fibersConsumed,
                fiberTarget = fiberTarget,
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
    val context = LocalContext.current
    val skinnyImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/skinny - no background.png")
        .build()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatCard(
                    label = "Calories",
                    value = "$caloriesConsumed",
                    target = "$calorieTarget",
                    unit = "kcal",
                    progress = (caloriesConsumed.toFloat() / calorieTarget).coerceIn(0f, 1f),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFood
                )
                StatCard(
                    label = "Water",
                    value = "$waterConsumedMl",
                    target = "$waterTargetMl",
                    unit = "ml",
                    progress = (waterConsumedMl.toFloat() / waterTargetMl).coerceIn(0f, 1f),
                    color = Color(0xFF34A9FF),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWater
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = skinnyImageRequest,
                contentDescription = "Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            FloatingActionButton(
                onClick = onOpenFood,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = "Food", modifier = Modifier.size(32.dp))
            }

            FloatingActionButton(
                onClick = onOpenWater,
                containerColor = Color(0xFFD1E9FF),
                contentColor = Color(0xFF0066CC),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.LocalDrink, contentDescription = "Water", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    target: String,
    unit: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: $target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    sugarsConsumed: Int,
    sugarTarget: Int,
    fibersConsumed: Int,
    fiberTarget: Int,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onManualAddFood: (FoodEntry) -> Unit,
) {
    var shouldShowAddFoodSheet by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    MacroOverviewPanel(
                        caloriesConsumed, calorieTarget,
                        proteinConsumed, proteinTarget,
                        carbsConsumed, carbsTarget,
                        fatsConsumed, fatTarget,
                        sugarsConsumed, sugarTarget,
                        fibersConsumed, fiberTarget
                    )
                }

                item {
                    Text(
                        text = "Today's Meals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(MealType.entries) { mealType ->
                    MealCard(
                        mealType = mealType,
                        mealFoods = foods.filter { it.meal == mealType },
                        onAddClick = {
                            shouldShowAddFoodSheet = true
                        }
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = onOpenCamera,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Label")
            }
            FloatingActionButton(
                onClick = { shouldShowAddFoodSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Manually")
            }
        }
    }

    if (shouldShowAddFoodSheet) {
        AddFoodSheet(
            onDismiss = { shouldShowAddFoodSheet = false },
            onAddFood = { food ->
                onManualAddFood(food)
                shouldShowAddFoodSheet = false
            },
        )
    }
}

@Composable
private fun MacroOverviewPanel(
    calories: Int, calorieTarget: Int,
    protein: Int, proteinTarget: Int,
    carbs: Int, carbsTarget: Int,
    fats: Int, fatTarget: Int,
    sugars: Int, sugarTarget: Int,
    fibers: Int, fiberTarget: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$calories",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / $calorieTarget kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { (calories.toFloat() / calorieTarget).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${(calories.toFloat() / calorieTarget * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroMiniStat("Protein", protein, proteinTarget, Color(0xFFE91E63))
                MacroMiniStat("Carbs", carbs, carbsTarget, Color(0xFF4CAF50))
                MacroMiniStat("Fats", fats, fatTarget, Color(0xFFFF9800))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryMacroStat("Sugars", sugars, sugarTarget, modifier = Modifier.weight(1f))
                SecondaryMacroStat("Fibers", fibers, fiberTarget, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroMiniStat(label: String, value: Int, target: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            CircularProgressIndicator(
                progress = { (value.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                trackColor = color.copy(alpha = 0.1f)
            )
            Text(
                text = "${value}g",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SecondaryMacroStat(label: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = "$value / $target g", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
private fun MealCard(
    mealType: MealType,
    mealFoods: List<FoodEntry>,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = mealType.icon, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = mealType.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val totalCals = mealFoods.sumOf { it.calories }
                        Text(
                            text = "$totalCals kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add to ${mealType.label}")
                }
            }

            if (mealFoods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                
                mealFoods.forEach { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "P ${food.protein}g • C ${food.carbs}g • F ${food.fats}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${food.calories} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTrackingPage(
    waterConsumedMl: Int,
    waterTargetMl: Int,
    onBack: () -> Unit,
    onAddWater: (Int) -> Unit,
) {
    val context = LocalContext.current
    val waterSvgRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/water_glass_icon.svg")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    val quantities = listOf(150, 250, 500)
    var selectedQuantity by rememberSaveable { mutableIntStateOf(250) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = "Water Intake",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(260.dp),
                shape = CircleShape,
                color = Color(0xFFE3F2FD).copy(alpha = 0.5f)
            ) {}
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = waterSvgRequest,
                    contentDescription = "Water Glass",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$waterConsumedMl / $waterTargetMl ml",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0288D1)
                )
                Text(
                    text = "Daily Goal",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF0288D1).copy(alpha = 0.7f)
                )
            }

            CircularProgressIndicator(
                progress = { (waterConsumedMl.toFloat() / waterTargetMl).coerceIn(0f, 1f) },
                modifier = Modifier.size(280.dp),
                color = Color(0xFF03A9F4),
                strokeWidth = 12.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                trackColor = Color(0xFFB3E5FC).copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    quantities.forEach { quantity ->
                        val isSelected = selectedQuantity == quantity
                        Surface(
                            onClick = { selectedQuantity = quantity },
                            modifier = Modifier.weight(1f).height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF03A9F4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$quantity\nml",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge,
                                    lineHeight = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onAddWater(selectedQuantity) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                ) {
                    Icon(Icons.Default.LocalDrink, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Add Water", style = MaterialTheme.typography.titleMedium)
                }
            }
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
        AddScannedFoodSheet(
            suggestion = scannedResult,
            onDismiss = { mScannedResult = null },
            onAddFood = { newFood ->
                onAddFoodFromScan(newFood)
                mScannedResult = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    onDismiss: () -> Unit,
    onAddFood: (FoodEntry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedMeal by rememberSaveable { mutableStateOf(MealType.Breakfast) }
    var name by rememberSaveable { mutableStateOf("") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var fatsText by rememberSaveable { mutableStateOf("") }
    var sugarsText by rememberSaveable { mutableStateOf("") }
    var fibersText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Add Food Manually",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name") },
                placeholder = { Text("e.g. Scrambled Eggs") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )

            Column {
                Text(text = "Select Meal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MealType.entries) { meal ->
                        FilterChip(
                            selected = selectedMeal == meal,
                            onClick = { selectedMeal = meal },
                            label = { Text(meal.label) },
                            leadingIcon = if (selectedMeal == meal) {
                                { Text(meal.icon) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Text(text = "Nutrition Facts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutritionField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter(Char::isDigit) },
                    label = "Calories",
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
                NutritionField(
                    value = proteinText,
                    onValueChange = { proteinText = it.filter(Char::isDigit) },
                    label = "Protein",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutritionField(
                    value = carbsText,
                    onValueChange = { carbsText = it.filter(Char::isDigit) },
                    label = "Carbs",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
                NutritionField(
                    value = fatsText,
                    onValueChange = { fatsText = it.filter(Char::isDigit) },
                    label = "Fats",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NutritionField(
                    value = sugarsText,
                    onValueChange = { sugarsText = it.filter(Char::isDigit) },
                    label = "Sugars",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
                NutritionField(
                    value = fibersText,
                    onValueChange = { fibersText = it.filter(Char::isDigit) },
                    label = "Fibers",
                    unit = "g",
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val calories = caloriesText.toIntOrNull() ?: 0
                    val protein = proteinText.toIntOrNull() ?: 0
                    val carbs = carbsText.toIntOrNull() ?: 0
                    val fats = fatsText.toIntOrNull() ?: 0
                    val sugars = sugarsText.toIntOrNull() ?: 0
                    val fibers = fibersText.toIntOrNull() ?: 0
                    val food = FoodEntry(
                        name = name.ifBlank { "Unnamed food" },
                        meal = selectedMeal,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fats = fats,
                        sugars = sugars,
                        fibers = fibers,
                        isHealthy = calories in 1..500,
                    )
                    onAddFood(food)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Food Entry", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun NutritionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(unit, style = MaterialTheme.typography.bodySmall) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScannedFoodSheet(
    suggestion: NutritionLabelScanResult,
    onDismiss: () -> Unit,
    onAddFood: (FoodEntry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMeal by rememberSaveable(suggestion.recognizedText) { mutableStateOf(MealType.Lunch) }
    var name by rememberSaveable(suggestion.recognizedText) { mutableStateOf(suggestion.name) }
    var selectedQuantityOption by rememberSaveable(suggestion.recognizedText) {
        mutableStateOf(ScannedQuantityOption.Full)
    }
    var multipleServingsText by rememberSaveable(suggestion.recognizedText) { mutableStateOf("2") }
    var gramsText by rememberSaveable(suggestion.recognizedText) { mutableStateOf("100") }

    val selectedQuantity = calculateScannedQuantity(
        option = selectedQuantityOption,
        multipleServingsText = multipleServingsText,
        gramsText = gramsText,
    )
    val baseCalories = if (selectedQuantity.usesPerServingValues) suggestion.calories else suggestion.caloriesPer100g
    val baseProtein = if (selectedQuantity.usesPerServingValues) suggestion.protein else suggestion.proteinPer100g
    val baseCarbs = if (selectedQuantity.usesPerServingValues) suggestion.carbs else suggestion.carbsPer100g
    val baseFats = if (selectedQuantity.usesPerServingValues) suggestion.fats else suggestion.fatsPer100g
    val baseSugars = if (selectedQuantity.usesPerServingValues) suggestion.sugars else suggestion.sugarsPer100g
    val baseFibers = if (selectedQuantity.usesPerServingValues) suggestion.fibers else suggestion.fibersPer100g
    val scaledCalories = scaleNutritionValue(baseCalories, selectedQuantity.multiplier)
    val scaledProtein = scaleNutritionValue(baseProtein, selectedQuantity.multiplier)
    val scaledCarbs = scaleNutritionValue(baseCarbs, selectedQuantity.multiplier)
    val scaledFats = scaleNutritionValue(baseFats, selectedQuantity.multiplier)
    val scaledSugars = scaleNutritionValue(baseSugars, selectedQuantity.multiplier)
    val scaledFibers = scaleNutritionValue(baseFibers, selectedQuantity.multiplier)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Verify Scanned Food",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    MealTypeSelector(
                        selectedMeal = selectedMeal,
                        onMealSelected = { selectedMeal = it },
                    )
                }
            }

            Column {
                Text(text = "Select Quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScannedQuantityOption.entries) { option ->
                        FilterChip(
                            selected = selectedQuantityOption == option,
                            onClick = { selectedQuantityOption = option },
                            label = { Text(option.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                when (selectedQuantityOption) {
                    ScannedQuantityOption.Multiple -> {
                        OutlinedTextField(
                            value = multipleServingsText,
                            onValueChange = { value ->
                                multipleServingsText = value.filter { char ->
                                    char.isDigit() || char == '.' || char == ','
                                }
                            },
                            label = { Text("Number of servings") },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                    ScannedQuantityOption.Grams -> {
                        OutlinedTextField(
                            value = gramsText,
                            onValueChange = { value ->
                                gramsText = value.filter { char ->
                                    char.isDigit() || char == '.' || char == ','
                                }
                            },
                            label = { Text("Grams consumed") },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                    else -> Unit
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculated Nutrition",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NutritionValueLabel("Calories", "$scaledCalories", "kcal")
                        NutritionValueLabel("Protein", "$scaledProtein", "g")
                        NutritionValueLabel("Carbs", "$scaledCarbs", "g")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NutritionValueLabel("Fats", "$scaledFats", "g")
                        NutritionValueLabel("Sugars", "$scaledSugars", "g")
                        NutritionValueLabel("Fibers", "$scaledFibers", "g")
                    }
                }
            }

            Button(
                enabled = selectedQuantity.isValid,
                onClick = {
                    val food = FoodEntry(
                        name = name.ifBlank { "Scanned food" },
                        meal = selectedMeal,
                        calories = scaledCalories,
                        protein = scaledProtein,
                        carbs = scaledCarbs,
                        fats = scaledFats,
                        sugars = scaledSugars,
                        fibers = scaledFibers,
                        isHealthy = scaledCalories in 1..500,
                    )
                    onAddFood(food)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Add Scanned Food", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun NutritionValueLabel(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = "$label ($unit)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun calculateScannedQuantity(
    option: ScannedQuantityOption,
    multipleServingsText: String,
    gramsText: String,
): ScannedQuantity {
    return when (option) {
        ScannedQuantityOption.Quarter -> ScannedQuantity(
            multiplier = 0.25f,
            isValid = true,
            usesPerServingValues = true,
            summary = "1/4 serving",
        )
        ScannedQuantityOption.Half -> ScannedQuantity(
            multiplier = 0.5f,
            isValid = true,
            usesPerServingValues = true,
            summary = "1/2 serving",
        )
        ScannedQuantityOption.Full -> ScannedQuantity(
            multiplier = 1f,
            isValid = true,
            usesPerServingValues = true,
            summary = "1 serving",
        )
        ScannedQuantityOption.Multiple -> {
            val servings = parsePositiveDecimal(multipleServingsText)
            if (servings == null) {
                ScannedQuantity(
                    multiplier = 1f,
                    isValid = false,
                    usesPerServingValues = true,
                    summary = "Invalid quantity",
                )
            } else {
                ScannedQuantity(
                    multiplier = servings,
                    isValid = true,
                    usesPerServingValues = true,
                    summary = "${formatFloatForUi(servings)} servings",
                )
            }
        }
        ScannedQuantityOption.Grams -> {
            val grams = parsePositiveDecimal(gramsText)
            if (grams == null) {
                ScannedQuantity(
                    multiplier = 1f,
                    isValid = false,
                    usesPerServingValues = false,
                    summary = "Invalid quantity",
                )
            } else {
                ScannedQuantity(
                    multiplier = grams / 100f,
                    isValid = true,
                    usesPerServingValues = false,
                    summary = "${grams.roundToInt()} g",
                )
            }
        }
    }
}

private fun parsePositiveDecimal(value: String): Float? {
    val normalized = value.trim().replace(",", ".")
    val parsedValue = normalized.toFloatOrNull() ?: return null
    return parsedValue.takeIf { it > 0f }
}

private fun scaleNutritionValue(baseValue: Int, multiplier: Float): Int {
    return (baseValue * multiplier).roundToInt().coerceAtLeast(0)
}

private fun formatFloatForUi(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (rounded % 1f == 0f) {
        rounded.roundToInt().toString()
    } else {
        rounded.toString()
    }
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

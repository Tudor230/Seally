package com.example.seally.nutrition

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
import com.example.seally.data.local.entity.NutritionLogEntity
import com.example.seally.data.repository.DailyGoalProgressRepository
import com.example.seally.data.repository.NutritionFoodEntryRepository
import com.example.seally.data.repository.NutritionLogRepository
import com.example.seally.data.repository.TargetRepository
import com.example.seally.ui.components.AppScreenBackground
import com.example.seally.ui.components.TopHeader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

enum class NutritionPage {
    Kitchen,
    Food,
    Water,
    Camera,
}

enum class MealType(val label: String, val icon: String) {
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

data class FoodEntry(
    val id: String = "",
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

class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val mNutritionLogRepository = NutritionLogRepository(application)
    private val mNutritionFoodEntryRepository = NutritionFoodEntryRepository(application)
    private val mTargetRepository = TargetRepository(application)
    private val mDailyGoalProgressRepository = DailyGoalProgressRepository(application)
    private val mCurrentDate: String = LocalDate.now().toString()
    private var mPersistedFoodEntries: List<NutritionFoodEntryEntity> = emptyList()
    private var mPersistedWaterMl: Int = 0

    var mCurrentPage by mutableStateOf(NutritionPage.Kitchen)
        private set

    var mWaterConsumedMl by mutableIntStateOf(0)
        private set

    var mShouldShowSealCelebration by mutableStateOf(false)
        private set

    val mFoods = mutableStateListOf<FoodEntry>()

    private var mSealCelebrationJob: Job? = null

    init {
        observePersistedNutrition()
    }

    fun openFoodPage() {
        mCurrentPage = NutritionPage.Food
    }

    fun openWaterPage() {
        mCurrentPage = NutritionPage.Water
    }

    fun openCameraPage() {
        mCurrentPage = NutritionPage.Camera
    }

    fun addManualFood(foodEntry: FoodEntry) {
        viewModelScope.launch {
            mNutritionFoodEntryRepository.addEntry(
                date = mCurrentDate,
                name = foodEntry.name,
                meal = foodEntry.meal.name,
                calories = foodEntry.calories,
                protein = foodEntry.protein,
                carbs = foodEntry.carbs,
                fats = foodEntry.fats,
                sugars = foodEntry.sugars,
                fibers = foodEntry.fibers,
                isHealthy = foodEntry.isHealthy,
            )
            triggerSealCelebration()
        }
    }

    fun addScannedFood(foodEntry: FoodEntry) {
        viewModelScope.launch {
            mNutritionFoodEntryRepository.addEntry(
                date = mCurrentDate,
                name = foodEntry.name,
                meal = foodEntry.meal.name,
                calories = foodEntry.calories,
                protein = foodEntry.protein,
                carbs = foodEntry.carbs,
                fats = foodEntry.fats,
                sugars = foodEntry.sugars,
                fibers = foodEntry.fibers,
                isHealthy = foodEntry.isHealthy,
            )
            triggerSealCelebration()
            mCurrentPage = NutritionPage.Food
        }
    }

    fun addWater(addedAmount: Int) {
        viewModelScope.launch {
            mNutritionLogRepository.addWater(mCurrentDate, addedAmount)
        }
    }

    fun removeFood(foodEntry: FoodEntry) {
        viewModelScope.launch {
            mNutritionFoodEntryRepository.removeEntry(foodEntry.id)
        }
    }

    fun removeWater(removedAmount: Int) {
        viewModelScope.launch {
            mNutritionLogRepository.addWater(mCurrentDate, -removedAmount)
        }
    }

    fun canNavigateBackInNutrition(): Boolean = mCurrentPage != NutritionPage.Kitchen

    fun navigateBackInNutrition() {
        mCurrentPage = when (mCurrentPage) {
            NutritionPage.Kitchen -> NutritionPage.Kitchen
            NutritionPage.Food, NutritionPage.Water -> NutritionPage.Kitchen
            NutritionPage.Camera -> NutritionPage.Food
        }
    }

    private fun triggerSealCelebration() {
        mSealCelebrationJob?.cancel()
        mShouldShowSealCelebration = true
        mSealCelebrationJob = viewModelScope.launch {
            delay(2000)
            mShouldShowSealCelebration = false
        }
    }

    private fun observePersistedNutrition() {
        viewModelScope.launch {
            mNutritionLogRepository.observeByDate(mCurrentDate).collectLatest { log ->
                mWaterConsumedMl = log?.waterMl ?: 0
                mPersistedWaterMl = mWaterConsumedMl
                syncNutritionGoalsProgress()
            }
        }
        viewModelScope.launch {
            mNutritionFoodEntryRepository.observeByDate(mCurrentDate).collectLatest { entries ->
                mPersistedFoodEntries = entries
                mFoods.clear()
                mFoods.addAll(entries.map { it.toFoodEntry() })
                syncNutritionLogFromFoodEntries()
                syncNutritionGoalsProgress()
            }
        }
    }

    private suspend fun syncNutritionLogFromFoodEntries() {
        val mExisting = mNutritionLogRepository.getByDate(mCurrentDate)
        val mWaterMl = mExisting?.waterMl ?: mPersistedWaterMl
        mNutritionLogRepository.upsert(
            NutritionLogEntity(
                date = mCurrentDate,
                waterMl = mWaterMl.coerceAtLeast(0),
                caloriesKcal = mPersistedFoodEntries.sumOf { it.calories },
                proteinG = mPersistedFoodEntries.sumOf { it.protein }.toDouble(),
                carbsG = mPersistedFoodEntries.sumOf { it.carbs }.toDouble(),
                fatsG = mPersistedFoodEntries.sumOf { it.fats }.toDouble(),
                sugarG = mPersistedFoodEntries.sumOf { it.sugars }.toDouble(),
                fiberG = mPersistedFoodEntries.sumOf { it.fibers }.toDouble(),
            ),
        )
    }

    private fun syncNutritionGoalsProgress() {
        viewModelScope.launch {
            val calories = mPersistedFoodEntries.sumOf { it.calories }.toDouble()
            val protein = mPersistedFoodEntries.sumOf { it.protein }.toDouble()
            val carbs = mPersistedFoodEntries.sumOf { it.carbs }.toDouble()
            val fats = mPersistedFoodEntries.sumOf { it.fats }.toDouble()
            val sugars = mPersistedFoodEntries.sumOf { it.sugars }.toDouble()
            val fibers = mPersistedFoodEntries.sumOf { it.fibers }.toDouble()
            val water = mPersistedWaterMl.coerceAtLeast(0).toDouble()

            val goalProgressByName = mapOf(
                "WATER" to water,
                "CALORIES" to calories,
                "PROTEIN" to protein,
                "CARBS" to carbs,
                "FATS" to fats,
                "SUGARS" to sugars,
                "FIBERS" to fibers,
            )

            goalProgressByName.forEach { (goalName, progressValue) ->
                if (mTargetRepository.getByGoalName(goalName) != null) {
                    mDailyGoalProgressRepository.setProgress(
                        goalName = goalName,
                        date = mCurrentDate,
                        progressValue = progressValue,
                    )
                }
            }
        }
    }
}

private fun NutritionFoodEntryEntity.toFoodEntry(): FoodEntry {
    val parsedMeal = runCatching { MealType.valueOf(meal) }.getOrDefault(MealType.Breakfast)
    return FoodEntry(
        id = id,
        name = name,
        meal = parsedMeal,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fats = fats,
        sugars = sugars,
        fibers = fibers,
        isHealthy = isHealthy,
    )
}

@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    mViewModel: NutritionViewModel = viewModel(),
) {
    val calorieTarget = 2200
    val waterTargetMl = 2500
    val proteinTarget = 140
    val carbsTarget = 220
    val fatTarget = 70
    val sugarTarget = 50
    val fiberTarget = 30

    val currentPage = mViewModel.mCurrentPage
    val waterConsumedMl = mViewModel.mWaterConsumedMl
    val foods = mViewModel.mFoods

    val caloriesConsumed = foods.sumOf { it.calories }
    val proteinConsumed = foods.sumOf { it.protein }
    val carbsConsumed = foods.sumOf { it.carbs }
    val fatsConsumed = foods.sumOf { it.fats }
    val sugarsConsumed = foods.sumOf { it.sugars }
    val fibersConsumed = foods.sumOf { it.fibers }

    var foodPendingDeletion by remember { mutableStateOf<FoodEntry?>(null) }

    val context = LocalContext.current
    val mBackgroundAssetPath = when (currentPage) {
        NutritionPage.Kitchen -> "backgrounds/kitchen.png"
        NutritionPage.Food -> "backgrounds/food_track.png"
        NutritionPage.Water -> "backgrounds/water_trackpng.png"
        NutritionPage.Camera -> "backgrounds/form_validator.png"
    }
    val musclesImageRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/seals/muscles.png")
        .build()

    BackHandler(
        enabled = mViewModel.canNavigateBackInNutrition(),
        onBack = mViewModel::navigateBackInNutrition,
    )

    DisposableEffect(currentPage) {
        onDetailVisibilityChanged(currentPage == NutritionPage.Kitchen)
        onDispose {}
    }

    if (foodPendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { foodPendingDeletion = null },
            title = { Text("Remove Food?") },
            text = { Text("Are you sure you want to remove '${foodPendingDeletion?.name}' from your logs?") },
            confirmButton = {
                Button(
                    onClick = {
                        foodPendingDeletion?.let { mViewModel.removeFood(it) }
                        foodPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { foodPendingDeletion = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AppScreenBackground(assetPath = mBackgroundAssetPath)

        Column(modifier = Modifier.fillMaxSize()) {
            if (currentPage == NutritionPage.Kitchen) {
                TopHeader(
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentPage) {
                    NutritionPage.Kitchen -> KitchenMainPage(
                        caloriesConsumed = caloriesConsumed,
                        calorieTarget = calorieTarget,
                        waterConsumedMl = waterConsumedMl,
                        waterTargetMl = waterTargetMl,
                        onOpenFood = mViewModel::openFoodPage,
                        onOpenWater = mViewModel::openWaterPage,
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
                        onBack = mViewModel::navigateBackInNutrition,
                        onOpenCamera = mViewModel::openCameraPage,
                        onManualAddFood = mViewModel::addManualFood,
                        onRemoveFood = { foodPendingDeletion = it },
                    )
                    NutritionPage.Water -> WaterTrackingPage(
                        waterConsumedMl = waterConsumedMl,
                        waterTargetMl = waterTargetMl,
                        onBack = mViewModel::navigateBackInNutrition,
                        onAddWater = mViewModel::addWater,
                        onRemoveWater = mViewModel::removeWater,
                    )
                    NutritionPage.Camera -> CameraTrackingPage(
                        onBack = mViewModel::navigateBackInNutrition,
                        onAddFoodFromScan = mViewModel::addScannedFood,
                    )
                }
            }
        }

        if (currentPage == NutritionPage.Kitchen) {
            AsyncImage(
                model = musclesImageRequest,
                contentDescription = "Seal Character",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(0.75f)
                    .padding(bottom = 20.dp)
            )
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
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        ) {
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWater
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // --- Action Buttons closer to margins ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
    onRemoveFood: (FoodEntry) -> Unit,
) {
    var shouldShowAddFoodSheet by rememberSaveable { mutableStateOf(false) }
    var pendingMealType by rememberSaveable { mutableStateOf<MealType?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Food Tracking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(MealType.entries) { mealType ->
                    MealCard(
                        mealType = mealType,
                        mealFoods = foods.filter { it.meal == mealType },
                        onRemoveFood = onRemoveFood,
                        onAddClick = {
                            pendingMealType = mealType
                            shouldShowAddFoodSheet = true
                        },
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
        }
    }

    if (shouldShowAddFoodSheet) {
        AddFoodSheet(
            initialMealType = pendingMealType,
            onDismiss = { 
                shouldShowAddFoodSheet = false
                pendingMealType = null
            },
            onAddFood = { food ->
                onManualAddFood(food)
                shouldShowAddFoodSheet = false
                pendingMealType = null
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
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
                        color = MaterialTheme.colorScheme.onSurface
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val proteinColor = if (isSystemInDarkTheme()) Color(0xFFFF80AB) else Color(0xFFE91E63)
                val carbsColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF4CAF50)
                val fatsColor = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFFF9800)
                
                MacroMiniStat("Protein", protein, proteinTarget, proteinColor)
                MacroMiniStat("Carbs", carbs, carbsTarget, carbsColor)
                MacroMiniStat("Fats", fats, fatTarget, fatsColor)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SecondaryMacroStat(label: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$value / $target g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onAddClick: () -> Unit,
    onRemoveFood: (FoodEntry) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val totalCals = mealFoods.sumOf { it.calories }
                        Text(
                            text = "$totalCals kcal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add to ${mealType.label}",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
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
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "P ${food.protein}g • C ${food.carbs}g • F ${food.fats}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${food.calories} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { onRemoveFood(food) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                )
                            }
                        }
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
    onRemoveWater: (Int) -> Unit,
) {
    val context = LocalContext.current
    val waterSvgRequest = ImageRequest.Builder(context)
        .data("file:///android_asset/icons/water_glass.svg")
        .decoderFactory(SvgDecoder.Factory())
        .build()

    val quantities = listOf(150, 250, 500)
    var selectedQuantity by rememberSaveable { mutableIntStateOf(250) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    var isRemoveMode by rememberSaveable { mutableStateOf(false) }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(if (isRemoveMode) "Remove Custom Amount" else "Add Custom Amount") },
            text = {
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter(Char::isDigit) },
                    label = { Text("Amount (ml)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customAmountText.toIntOrNull() ?: 0
                        if (amount > 0) {
                            if (isRemoveMode) onRemoveWater(amount) else onAddWater(amount)
                            showCustomDialog = false
                            customAmountText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isRemoveMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (isRemoveMode) "Remove" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Water Intake",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Mode Toggle
            Surface(
                onClick = { isRemoveMode = !isRemoveMode },
                shape = RoundedCornerShape(12.dp),
                color = if (isRemoveMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = if (isRemoveMode) "Remove Mode" else "Add Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRemoveMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                color = if (isRemoveMode) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRemoveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Daily Goal",
                    style = MaterialTheme.typography.labelLarge,
                    color = (if (isRemoveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.7f)
                )
            }

            CircularProgressIndicator(
                progress = { (waterConsumedMl.toFloat() / waterTargetMl).coerceIn(0f, 1f) },
                modifier = Modifier.size(280.dp),
                color = if (isRemoveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                trackColor = (if (isRemoveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                            color = if (isSelected) {
                                if (isRemoveMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) {
                                if (isRemoveMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            } else MaterialTheme.colorScheme.onSurfaceVariant
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
                    
                    // Custom Button
                    Surface(
                        onClick = { showCustomDialog = true },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Custom",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (isRemoveMode) onRemoveWater(selectedQuantity) else onAddWater(selectedQuantity) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = if (isRemoveMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(if (isRemoveMode) Icons.Default.Close else Icons.Default.LocalDrink, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isRemoveMode) "Remove Water" else "Add Water", style = MaterialTheme.typography.titleMedium)
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
        modifier = Modifier
            .fillMaxSize(),
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
    initialMealType: MealType? = null,
    onDismiss: () -> Unit,
    onAddFood: (FoodEntry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedMeal by rememberSaveable { mutableStateOf(initialMealType ?: MealType.Breakfast) }
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

            if (initialMealType == null) {
                Column {
                    Text(text = "Select Meal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernSegmentedSelector(
                        options = MealType.entries,
                        selectedOption = selectedMeal,
                        onOptionSelected = { selectedMeal = it },
                        labelProvider = { it.label },
                        iconProvider = { it.icon }
                    )
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

    var selectedMeal by rememberSaveable(suggestion.recognizedText) { 
        mutableStateOf(suggestion.mealType ?: MealType.Lunch) 
    }
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = "Verify Scanned Food",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Food Name Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PRODUCT INFO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            // 2. Meal Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ASSIGN TO MEAL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ModernSegmentedSelector(
                    options = MealType.entries,
                    selectedOption = selectedMeal,
                    onOptionSelected = { selectedMeal = it },
                    labelProvider = { it.label },
                    iconProvider = { it.icon }
                )
            }

            // 3. Quantity Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PORTION SIZE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ModernSegmentedSelector(
                    options = ScannedQuantityOption.entries,
                    selectedOption = selectedQuantityOption,
                    onOptionSelected = { selectedQuantityOption = it },
                    labelProvider = { it.label }
                )
                
                AnimatedContent(
                    targetState = selectedQuantityOption,
                    transitionSpec = {
                        fadeIn() + slideInVertically { it / 2 } togetherWith fadeOut() + slideOutVertically { it / 2 }
                    },
                    label = "QuantityInputAnimation"
                ) { targetOption ->
                    when (targetOption) {
                        ScannedQuantityOption.Multiple -> {
                            OutlinedTextField(
                                value = multipleServingsText,
                                onValueChange = { value ->
                                    multipleServingsText = value.filter { char ->
                                        char.isDigit() || char == '.' || char == ','
                                    }
                                },
                                label = { Text("Number of servings") },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                suffix = { Text("g") }
                            )
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // 4. Results Summary Section
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "CALCULATED NUTRITION",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NutritionValueLabel("Calories", "$scaledCalories", "kcal")
                        NutritionValueLabel("Protein", "$scaledProtein", "g")
                        NutritionValueLabel("Carbs", "$scaledCarbs", "g")
                    }
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
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Add Food Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun <T> ModernSegmentedSelector(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    iconProvider: ((T) -> String)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Surface(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            iconProvider?.let { Text(text = it(option), fontSize = 14.sp) }
                            Text(
                                text = labelProvider(option),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
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

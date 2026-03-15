package com.example.seally.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.repository.ExerciseLogRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.ui.components.AppScreenBackground
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray

private data class ExerciseEntry(
    val name: String,
    val metric: String,
    val value: String,
)

private data class ExerciseCatalogEntry(
    val name: String,
    val metric: String,
)

private val FALLBACK_CATALOG_ENTRY = ExerciseCatalogEntry(name = "Push-ups", metric = "reps")

private fun defaultExerciseEntry(catalog: List<ExerciseCatalogEntry>): ExerciseEntry {
    val mDefaultEntry = catalog.firstOrNull() ?: FALLBACK_CATALOG_ENTRY
    return ExerciseEntry(
        name = mDefaultEntry.name,
        metric = mDefaultEntry.metric,
        value = "",
    )
}

private fun catalogEntryFor(
    catalog: List<ExerciseCatalogEntry>,
    name: String,
): ExerciseCatalogEntry? {
    return catalog.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val mExerciseLogRepository = remember(context) { ExerciseLogRepository(context.applicationContext) }
    val mExerciseCatalog = remember(context) { loadExerciseCatalog(context) }
    val today = remember { LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    val mExerciseLogs by mExerciseLogRepository.observeAll().collectAsState(initial = emptyList())
    val workoutByDate = remember(mExerciseLogs, mExerciseCatalog) {
        mExerciseLogs.toWorkoutMap(mExerciseCatalog)
    }
    var isEntryDialogOpen by rememberSaveable { mutableStateOf(false) }
    val draftExercises = remember { mutableStateListOf<ExerciseEntry>() }

    val pageCount = 1200
    val startPage = pageCount / 2
    val startMonth = remember { YearMonth.from(today) }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    val currentMonth = remember(pagerState.currentPage) {
        startMonth.plusMonths((pagerState.currentPage - startPage).toLong())
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AppScreenBackground(assetPath = "backgrounds/calendar.png")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = monthTitle(currentMonth),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
            }

            // Calendar Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DaysOfWeekHeader()
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        val month = startMonth.plusMonths((page - startPage).toLong())
                        MonthGrid(
                            month = month,
                            selectedDate = selectedDate,
                            today = today,
                            workoutDates = workoutByDate.keys,
                            onDayClick = { clicked ->
                                selectedDate = clicked
                                draftExercises.clear()
                                workoutByDate[clicked]?.let { existing -> draftExercises.addAll(existing) }
                                if (draftExercises.isEmpty()) {
                                    draftExercises.add(defaultExerciseEntry(mExerciseCatalog))
                                }
                                isEntryDialogOpen = true
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity for ${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(onClick = {
                    draftExercises.clear()
                    workoutByDate[selectedDate]?.let { existing -> draftExercises.addAll(existing) }
                    if (draftExercises.isEmpty()) {
                        draftExercises.add(defaultExerciseEntry(mExerciseCatalog))
                    }
                    isEntryDialogOpen = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Plan", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dayWorkouts = workoutByDate[selectedDate]
            if (dayWorkouts.isNullOrEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "No training scheduled for today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dayWorkouts.forEach { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = entry.name.ifBlank { "Unnamed Exercise" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${entry.value.ifBlank { "0" }} ${entry.metric}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (isEntryDialogOpen) {
        WorkoutListDialog(
            date = selectedDate,
            exerciseCatalog = mExerciseCatalog,
            draftExercises = draftExercises,
            onDismiss = { isEntryDialogOpen = false },
            onClear = {
                val mDate = selectedDate.toString()
                scope.launch {
                    mExerciseLogRepository.deleteByDate(mDate)
                    isEntryDialogOpen = false
                }
            },
            onSave = {
                val mDate = selectedDate.toString()
                val mEntriesToPersist = draftExercises
                    .mapNotNull { entry ->
                        val mQuantity = entry.value.toDoubleOrNull() ?: return@mapNotNull null
                        if (mQuantity <= 0.0) return@mapNotNull null
                        entry to mQuantity
                    }
                scope.launch {
                    mExerciseLogRepository.deleteByDate(mDate)
                    mEntriesToPersist.forEach { (entry, quantity) ->
                        mExerciseLogRepository.addLog(
                            exerciseName = entry.name,
                            quantity = quantity,
                            metric = entry.metric,
                            date = mDate,
                        )
                    }
                    isEntryDialogOpen = false
                }
            },
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        ).forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    workoutDates: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = (firstOfMonth.dayOfWeek.value - 1) % 7
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasWorkout = workoutDates.contains(date),
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasWorkout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                ),
            )
            if (hasWorkout) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutListDialog(
    date: LocalDate,
    exerciseCatalog: List<ExerciseCatalogEntry>,
    draftExercises: androidx.compose.runtime.snapshots.SnapshotStateList<ExerciseEntry>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    var mExpandedExerciseIndex by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Training Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                draftExercises.forEachIndexed { index, entry ->
                    val mIsExerciseMenuExpanded = mExpandedExerciseIndex == index
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExposedDropdownMenuBox(
                                    expanded = mIsExerciseMenuExpanded,
                                    onExpandedChange = { isExpanded ->
                                        mExpandedExerciseIndex = if (isExpanded) index else null
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    OutlinedTextField(
                                        value = entry.name,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Exercise") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = mIsExerciseMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = mIsExerciseMenuExpanded,
                                        onDismissRequest = { mExpandedExerciseIndex = null },
                                    ) {
                                        exerciseCatalog.forEach { catalogEntry ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${catalogEntry.name} (${catalogEntry.metric})")
                                                },
                                                onClick = {
                                                    draftExercises[index] = entry.copy(
                                                        name = catalogEntry.name,
                                                        metric = catalogEntry.metric,
                                                    )
                                                    mExpandedExerciseIndex = null
                                                },
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    if (mExpandedExerciseIndex == index) {
                                        mExpandedExerciseIndex = null
                                    }
                                    if (draftExercises.size > 1) draftExercises.removeAt(index)
                                    else draftExercises[index] = defaultExerciseEntry(exerciseCatalog)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            OutlinedTextField(
                                value = entry.value,
                                onValueChange = { nextValue ->
                                    draftExercises[index] = entry.copy(value = sanitizeMetricValueInput(nextValue))
                                },
                                label = { Text(entry.metric.replaceFirstChar { it.titlecase() }) },
                                suffix = { Text(entry.metric) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                            )
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = { draftExercises.add(defaultExerciseEntry(exerciseCatalog)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, shape = RoundedCornerShape(12.dp)) { Text("Save Plan") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("Clear Plan", color = MaterialTheme.colorScheme.error) }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

private fun monthTitle(month: YearMonth): String {
    return "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
}

private fun List<ExerciseLogEntity>.toWorkoutMap(
    catalog: List<ExerciseCatalogEntry>,
): Map<LocalDate, List<ExerciseEntry>> {
    return this
        .mapNotNull { log ->
            val mDate = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@mapNotNull null
            mDate to log.toExerciseEntry(catalog)
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )
}

private fun ExerciseLogEntity.toExerciseEntry(
    catalog: List<ExerciseCatalogEntry>,
): ExerciseEntry {
    val mCatalogEntry = catalogEntryFor(catalog, exerciseName)
    val mResolvedMetric = when {
        metric.startsWith("sets:") -> "reps"
        metric.isNotBlank() -> metric
        mCatalogEntry != null -> mCatalogEntry.metric
        else -> "units"
    }
    return ExerciseEntry(
        name = exerciseName,
        metric = mResolvedMetric,
        value = formatMetricValue(quantity),
    )
}

private fun loadExerciseCatalog(context: android.content.Context): List<ExerciseCatalogEntry> {
    val mLoadedCatalog = runCatching {
        val mJsonText = context.assets.open("data/home_exercise_catalog.json").bufferedReader().use { it.readText() }
        val mJsonArray = JSONArray(mJsonText)
        buildList {
            for (index in 0 until mJsonArray.length()) {
                val item = mJsonArray.getJSONObject(index)
                val mName = item.optString("name").trim()
                val mMetric = item.optString("metric").trim()
                if (mName.isNotBlank() && mMetric.isNotBlank()) {
                    add(ExerciseCatalogEntry(name = mName, metric = mMetric))
                }
            }
        }
    }.getOrDefault(emptyList())
    return if (mLoadedCatalog.isNotEmpty()) {
        mLoadedCatalog
    } else {
        listOf(FALLBACK_CATALOG_ENTRY)
    }
}

private fun sanitizeMetricValueInput(input: String): String {
    val mFiltered = input.filter { it.isDigit() || it == '.' }
    val mFirstDot = mFiltered.indexOf('.')
    if (mFirstDot == -1) return mFiltered
    val mWithoutExtraDots = buildString {
        append(mFiltered.substring(0, mFirstDot + 1))
        append(mFiltered.substring(mFirstDot + 1).replace(".", ""))
    }
    return mWithoutExtraDots
}

private fun formatMetricValue(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}

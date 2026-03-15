package com.example.seally.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.seally.ui.components.AppScreenBackground
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

enum class CalendarSubPage {
    CALENDAR,
    PRESET_MANAGER,
    PRESET_EDITOR,
    WORKOUT_PLANNER,
    EXERCISE_PICKER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: CalendarViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var currentSubPage by rememberSaveable { mutableStateOf(CalendarSubPage.CALENDAR) }
    var editingPreset by remember { mutableStateOf<TrainingPresetUiModel?>(null) }
    var exercisePickerReturnPage by rememberSaveable { mutableStateOf(CalendarSubPage.WORKOUT_PLANNER) }
    var presetDraftName by rememberSaveable { mutableStateOf("") }
    
    val selectedDate by viewModel.selectedDate.collectAsState()
    val workoutByDate by viewModel.workoutByDate.collectAsState()
    val planByDate by viewModel.planByDate.collectAsState()
    val presets by viewModel.presets.collectAsState()
    
    val today = remember { LocalDate.now() }

    val draftExercises = remember { mutableStateListOf<ExerciseEntry>() }
    var exerciseTargetIndex by remember { mutableStateOf<Int?>(null) }

    // Navigation logic
    BackHandler(enabled = currentSubPage != CalendarSubPage.CALENDAR) {
        currentSubPage = when (currentSubPage) {
            CalendarSubPage.PRESET_EDITOR -> CalendarSubPage.PRESET_MANAGER
            CalendarSubPage.EXERCISE_PICKER -> exercisePickerReturnPage
            CalendarSubPage.WORKOUT_PLANNER -> CalendarSubPage.CALENDAR
            CalendarSubPage.PRESET_MANAGER -> CalendarSubPage.CALENDAR
            else -> CalendarSubPage.CALENDAR
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppScreenBackground(assetPath = "backgrounds/calendar.png", overlayTransparency = 0.15f)

        AnimatedContent(
            targetState = currentSubPage,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "SubPageNavigation",
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                CalendarSubPage.CALENDAR -> {
                    MainCalendarContent(
                        selectedDate = selectedDate,
                        workoutByDate = workoutByDate,
                        planByDate = planByDate,
                        today = today,
                        onBackClick = onBackClick,
                        onDaySelected = { date -> viewModel.selectDate(date) },
                        onOpenPlanner = {
                            editingPreset = null
                            draftExercises.clear()
                            val planForDay = planByDate[selectedDate].orEmpty()
                            val workoutForDay = workoutByDate[selectedDate].orEmpty()
                            val existing = when {
                                selectedDate.isBefore(today) -> workoutForDay
                                selectedDate == today && workoutForDay.isNotEmpty() -> workoutForDay
                                else -> planForDay.ifEmpty { workoutForDay }
                            }
                            draftExercises.addAll(existing)
                            currentSubPage = CalendarSubPage.WORKOUT_PLANNER
                        },
                        onOpenPresets = { currentSubPage = CalendarSubPage.PRESET_MANAGER }
                    )
                }
                CalendarSubPage.PRESET_MANAGER -> {
                    PresetManagerPage(
                        presets = presets,
                        onBackClick = { currentSubPage = CalendarSubPage.CALENDAR },
                        onEditPreset = { preset ->
                            editingPreset = preset
                            presetDraftName = preset.name
                            draftExercises.clear()
                            draftExercises.addAll(preset.exercises)
                            currentSubPage = CalendarSubPage.PRESET_EDITOR
                        },
                        onDeletePreset = { viewModel.deletePreset(it) },
                        onAddPreset = {
                            editingPreset = null
                            presetDraftName = ""
                            draftExercises.clear()
                            currentSubPage = CalendarSubPage.PRESET_EDITOR
                        }
                    )
                }
                CalendarSubPage.PRESET_EDITOR -> {
                    PresetEditorPage(
                        initialPreset = editingPreset,
                        name = presetDraftName,
                        onNameChange = { presetDraftName = it },
                        draftExercises = draftExercises,
                        onBackClick = { currentSubPage = CalendarSubPage.PRESET_MANAGER },
                        onSave = { name, exercises ->
                            viewModel.savePreset(editingPreset?.id, name, exercises) { didSave ->
                                if (didSave) {
                                    presetDraftName = ""
                                    currentSubPage = CalendarSubPage.PRESET_MANAGER
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Preset needs a name and at least one valid exercise.")
                                    }
                                }
                            }
                        },
                        onPickExercise = { index ->
                            exerciseTargetIndex = index
                            exercisePickerReturnPage = CalendarSubPage.PRESET_EDITOR
                            currentSubPage = CalendarSubPage.EXERCISE_PICKER
                        }
                    )
                }
                CalendarSubPage.WORKOUT_PLANNER -> {
                    WorkoutPlannerPage(
                        date = selectedDate,
                        presets = presets,
                        draftExercises = draftExercises,
                        onBackClick = { currentSubPage = CalendarSubPage.CALENDAR },
                        onApplyPreset = { preset -> 
                            // Append preset exercises and mark them with presetName
                            draftExercises.addAll(preset.exercises.map { it.copy(presetName = preset.name) })
                        },
                        onClear = {
                            viewModel.clearPlan(selectedDate)
                            currentSubPage = CalendarSubPage.CALENDAR
                        },
                        onSave = {
                            viewModel.savePlan(selectedDate, draftExercises.toList())
                            currentSubPage = CalendarSubPage.CALENDAR
                        },
                        onPickExercise = { index ->
                            exerciseTargetIndex = index
                            exercisePickerReturnPage = CalendarSubPage.WORKOUT_PLANNER
                            currentSubPage = CalendarSubPage.EXERCISE_PICKER
                        }
                    )
                }
                CalendarSubPage.EXERCISE_PICKER -> {
                    ExercisePickerPage(
                        catalog = viewModel.exerciseCatalog,
                        onBackClick = {
                            currentSubPage = exercisePickerReturnPage
                        },
                        onPick = { catalogEntry ->
                            val index = exerciseTargetIndex
                            if (index != null && index < draftExercises.size) {
                                draftExercises[index] = draftExercises[index].copy(
                                    name = catalogEntry.name,
                                    metric = catalogEntry.metric
                                )
                            }
                            currentSubPage = exercisePickerReturnPage
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
        )
    }
}

@Composable
fun MainCalendarContent(
    selectedDate: LocalDate,
    workoutByDate: Map<LocalDate, List<ExerciseEntry>>,
    planByDate: Map<LocalDate, List<ExerciseEntry>>,
    today: LocalDate,
    onBackClick: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenPresets: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val startMonth = remember { YearMonth.from(today) }
    val pageCount = 1200
    val startPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { pageCount })
    val currentMonth = remember(pagerState.currentPage) {
        startMonth.plusMonths((pagerState.currentPage - startPage).toLong())
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = currentMonth.year.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Text(text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            }
            Row {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DaysOfWeekHeader()
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, pageSpacing = 16.dp) { page ->
                    val month = startMonth.plusMonths((page - startPage).toLong())
                    MonthGrid(month = month, selectedDate = selectedDate, today = today, workoutDates = workoutByDate.keys, planDates = planByDate.keys, onDayClick = onDaySelected)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        DaySummaryCard(
            date = selectedDate, 
            today = today, 
            workouts = workoutByDate[selectedDate].orEmpty(), 
            plans = planByDate[selectedDate].orEmpty(), 
            onOpenPlanner = onOpenPlanner
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpenPresets, 
                    modifier = Modifier.weight(0.4f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Presets", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenPlanner,
                    modifier = Modifier.weight(0.6f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Plans", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DaySummaryCard(date: LocalDate, today: LocalDate, workouts: List<ExerciseEntry>, plans: List<ExerciseEntry>, onOpenPlanner: () -> Unit) {
    val displayExercises = when {
        date.isBefore(today) -> workouts
        date == today -> if (workouts.isNotEmpty()) workouts else plans
        else -> plans
    }
    
    val isCompleted = date.isBefore(today) || (date == today && workouts.isNotEmpty())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onOpenPlanner() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, 
            if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.secondaryContainer, 
                            CircleShape
                        ), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Event, 
                        contentDescription = null, 
                        tint = if (isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer, 
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            date == today -> "Today"
                            date == today.plusDays(1) -> "Tomorrow"
                            date == today.minusDays(1) -> "Yesterday"
                            else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        }, 
                        style = MaterialTheme.typography.labelMedium, 
                        color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (displayExercises.isNotEmpty()) {
                val groupedInSummary = displayExercises.groupBy { it.presetName }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groupedInSummary.forEach { (presetName, exercises) ->
                        Column {
                            if (presetName != null) {
                                Text(
                                    text = presetName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            exercises.forEach { exercise ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(if (presetName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = exercise.name, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${exercise.value} ${exercise.metric}", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No workouts planned for this day.", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarSubPageHeader(
    title: String,
    onBackClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PresetManagerPage(presets: List<TrainingPresetUiModel>, onBackClick: () -> Unit, onEditPreset: (TrainingPresetUiModel) -> Unit, onDeletePreset: (String) -> Unit, onAddPreset: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CalendarSubPageHeader(
            title = "My Presets",
            onBackClick = onBackClick,
        )
        if (presets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No presets saved yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(presets) { preset ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onEditPreset(preset) },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "${preset.exercises.size} exercises", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeletePreset(preset.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(onClick = onAddPreset, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Preset", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PresetEditorPage(
    initialPreset: TrainingPresetUiModel?,
    name: String,
    onNameChange: (String) -> Unit,
    draftExercises: androidx.compose.runtime.snapshots.SnapshotStateList<ExerciseEntry>,
    onBackClick: () -> Unit,
    onSave: (String, List<ExerciseEntry>) -> Unit,
    onPickExercise: (Int) -> Unit,
) {
    val canSave by remember(name) { derivedStateOf { name.trim().isNotBlank() } }
    Column(modifier = Modifier.fillMaxSize()) {
        CalendarSubPageHeader(
            title = if (initialPreset == null) "New Preset" else "Edit Preset",
            onBackClick = onBackClick,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Preset Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            if (draftExercises.isEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Box(contentAlignment = Alignment.Center) { Text("No exercises added yet", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
                }
            } else {
                draftExercises.forEachIndexed { index, entry ->
                    ExerciseEditItem(entry = entry, onUpdate = { draftExercises[index] = it }, onRemove = { draftExercises.removeAt(index) }, onPickExercise = { onPickExercise(index) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { draftExercises.add(ExerciseEntry("", "reps", "")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Exercise")
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(
                onClick = { onSave(name.trim(), draftExercises.toList()) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Save Preset", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlannerPage(
    date: LocalDate, 
    presets: List<TrainingPresetUiModel>, 
    draftExercises: androidx.compose.runtime.snapshots.SnapshotStateList<ExerciseEntry>, 
    onBackClick: () -> Unit, 
    onApplyPreset: (TrainingPresetUiModel) -> Unit, 
    onClear: () -> Unit, 
    onSave: () -> Unit, 
    onPickExercise: (Int) -> Unit
) {
    var showPresetPicker by remember { mutableStateOf(false) }
    
    // Group exercises by preset for visual organization
    val groupedExercises = remember(draftExercises.toList()) {
        val groups = mutableListOf<Pair<String?, MutableList<ExerciseEntry>>>()
        var currentGroup: MutableList<ExerciseEntry>? = null
        var currentPresetName: String? = null
        
        draftExercises.forEach { exercise ->
            if (exercise.presetName != currentPresetName || currentGroup == null) {
                currentPresetName = exercise.presetName
                currentGroup = mutableListOf()
                groups.add(currentPresetName to currentGroup!!)
            }
            currentGroup!!.add(exercise)
        }
        groups
    }

    val canSavePlan by remember(draftExercises.toList()) {
        derivedStateOf { draftExercises.any { it.isValidDraftExercise() } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CalendarSubPageHeader(
            title = "Plan Workout",
            subtitle = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
            onBackClick = onBackClick,
        )
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            if (presets.isNotEmpty()) {
                Surface(
                    onClick = { showPresetPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Load from Workout Preset", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(text = "Daily Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (draftExercises.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(120.dp), 
                    shape = RoundedCornerShape(24.dp), 
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), 
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No exercises planned yet", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium) 
                        }
                    }
                }
            } else {
                // Render grouped exercises
                groupedExercises.forEach { (presetName, exercises) ->
                    if (presetName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = presetName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    exercises.forEach { entry ->
                        // Find original index in draftExercises
                        val originalIndex = draftExercises.indexOf(entry)
                        if (originalIndex != -1) {
                            ExerciseEditItem(
                                entry = entry, 
                                onUpdate = { draftExercises[originalIndex] = it }, 
                                onRemove = { draftExercises.removeAt(originalIndex) }, 
                                onPickExercise = { onPickExercise(originalIndex) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row {
                OutlinedButton(
                    onClick = { draftExercises.add(ExerciseEntry("", "reps", "")) }, 
                    modifier = Modifier.weight(1f).height(48.dp), 
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Manual")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (draftExercises.isNotEmpty()) {
                TextButton(
                    onClick = onClear, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { 
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Exercises") 
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(
                onClick = onSave, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                enabled = canSavePlan, 
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) { 
                Text("Confirm Daily Plan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) 
            }
        }
    }
    
    if (showPresetPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPresetPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text(text = "Apply Training Preset", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(presets) { preset ->
                        Surface(
                            onClick = { onApplyPreset(preset); showPresetPicker = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = preset.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "${preset.exercises.size} exercises", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseEditItem(entry: ExerciseEntry, onUpdate: (ExerciseEntry) -> Unit, onRemove: () -> Unit, onPickExercise: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(20.dp), 
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), 
        tonalElevation = 2.dp, 
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onPickExercise, 
                    modifier = Modifier.weight(1f), 
                    shape = RoundedCornerShape(14.dp), 
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), 
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (entry.name.isBlank()) "Select Exercise..." else entry.name, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = if (entry.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface, 
                            fontWeight = if (entry.name.isBlank()) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) { 
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) 
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = entry.value,
                onValueChange = { onUpdate(entry.copy(value = sanitizeInput(it))) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text(entry.metric, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
fun ExercisePickerPage(catalog: List<ExerciseCatalogEntry>, onBackClick: () -> Unit, onPick: (ExerciseCatalogEntry) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCatalog = remember(searchQuery) {
        if (searchQuery.isBlank()) catalog
        else catalog.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        CalendarSubPageHeader(
            title = "Choose Exercise",
            onBackClick = onBackClick,
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Search exercises...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(filteredCatalog) { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onPick(entry) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = entry.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Measured in ${entry.metric}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun ExerciseEntry.isValidDraftExercise(): Boolean {
    return name.isNotBlank() && metric.isNotBlank() && value.toDoubleOrNull()?.let { it > 0.0 } == true
}

@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { dow ->
            Text(text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selectedDate: LocalDate, today: LocalDate, workoutDates: Set<LocalDate>, planDates: Set<LocalDate>, onDayClick: (LocalDate) -> Unit) {
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
                        DayCell(date = date, isSelected = date == selectedDate, isToday = date == today, hasWorkout = workoutDates.contains(date), hasPlan = planDates.contains(date), onClick = { onDayClick(date) }, modifier = Modifier.weight(1f))
                    } else { Spacer(modifier = Modifier.weight(1f).aspectRatio(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isSelected: Boolean, isToday: Boolean, hasWorkout: Boolean, hasPlan: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    hasWorkout -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    hasPlan -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium)
            )
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.height(6.dp)) {
                if (hasWorkout) { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)) }
                else if (hasPlan) { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)) }
            }
        }
    }
}

private fun sanitizeInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot == -1) return filtered
    return buildString {
        append(filtered.substring(0, firstDot + 1))
        append(filtered.substring(firstDot + 1).replace(".", ""))
    }
}

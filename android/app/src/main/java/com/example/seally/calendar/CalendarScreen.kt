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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private data class ExerciseEntry(
    val name: String,
    val sets: Int,
    val reps: Int,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val today = remember { LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    val workoutByDate = remember { mutableStateMapOf<LocalDate, List<ExerciseEntry>>() }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        monthTitle(currentMonth),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
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
                            draftExercises.add(ExerciseEntry(name = "", sets = 3, reps = 10))
                        }
                        isEntryDialogOpen = true
                    },
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            // Preview Section
            Text(
                text = "Plan for ${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            val dayWorkouts = workoutByDate[selectedDate]
            if (dayWorkouts.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No exercises planned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    dayWorkouts.forEach { entry ->
                        ListItem(
                            headlineContent = { Text(entry.name.ifBlank { "Unnamed Exercise" }) },
                            supportingContent = { Text("${entry.sets} sets × ${entry.reps} reps") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {}
                            }
                        )
                    }
                }
            }
        }
    }

    if (isEntryDialogOpen) {
        WorkoutListDialog(
            date = selectedDate,
            draftExercises = draftExercises,
            onDismiss = { isEntryDialogOpen = false },
            onClear = {
                workoutByDate.remove(selectedDate)
                isEntryDialogOpen = false
            },
            onSave = {
                workoutByDate[selectedDate] = draftExercises.toList()
                isEntryDialogOpen = false
            },
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        DayOfWeek.values().forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
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
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
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
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onBackground
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                ),
            )
            // Workout Indicator Dot
            if (hasWorkout) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

@Composable
private fun WorkoutListDialog(
    date: LocalDate,
    draftExercises: androidx.compose.runtime.snapshots.SnapshotStateList<ExerciseEntry>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Plan: ${date.dayOfMonth} ${date.month.name.lowercase()}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                draftExercises.forEachIndexed { index, entry ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = entry.name,
                                    onValueChange = { draftExercises[index] = entry.copy(name = it) },
                                    label = { Text("Exercise") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(onClick = {
                                    if (draftExercises.size > 1) draftExercises.removeAt(index)
                                    else draftExercises[index] = ExerciseEntry("", 3, 10)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = entry.sets.toString(),
                                    onValueChange = { draftExercises[index] = entry.copy(sets = it.toIntOrNull() ?: 0) },
                                    label = { Text("Sets") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = entry.reps.toString(),
                                    onValueChange = { draftExercises[index] = entry.copy(reps = it.toIntOrNull() ?: 0) },
                                    label = { Text("Reps") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { draftExercises.add(ExerciseEntry("", 3, 10)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Another Exercise")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, shape = RoundedCornerShape(8.dp)) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
        },
    )
}

private fun monthTitle(month: YearMonth): String {
    return "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
}
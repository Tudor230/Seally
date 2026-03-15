package com.example.seally.calendar

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.seally.data.local.entity.CalendarPlanEntryEntity
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.local.entity.TrainingPresetEntity
import com.example.seally.data.local.entity.TrainingPresetExerciseEntity
import com.example.seally.data.repository.CalendarDayCompletionRepository
import com.example.seally.data.repository.CalendarPlanInput
import com.example.seally.data.repository.CalendarPlanRepository
import com.example.seally.data.repository.ExerciseLogRepository
import com.example.seally.data.repository.TrainingPresetExerciseInput
import com.example.seally.data.repository.TrainingPresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth

data class ExerciseEntry(
    val name: String,
    val metric: String,
    val value: String,
    val sourceLogId: String? = null,
)

data class ExerciseCatalogEntry(
    val name: String,
    val metric: String,
)

data class TrainingPresetUiModel(
    val id: String,
    val name: String,
    val exercises: List<ExerciseEntry>,
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val exerciseLogRepository = ExerciseLogRepository(context)
    private val calendarPlanRepository = CalendarPlanRepository(context)
    private val completionRepository = CalendarDayCompletionRepository(context)
    private val presetRepository = TrainingPresetRepository(context)

    val exerciseCatalog: List<ExerciseCatalogEntry> = loadExerciseCatalog(context)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val exerciseLogs = exerciseLogRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val planEntries = calendarPlanRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetEntities = presetRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetExerciseEntities = presetRepository.observePresetExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutByDate: StateFlow<Map<LocalDate, List<ExerciseEntry>>> = exerciseLogs
        .combine(MutableStateFlow(exerciseCatalog)) { logs, catalog ->
            logs.toWorkoutMap(catalog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val planByDate: StateFlow<Map<LocalDate, List<ExerciseEntry>>> = planEntries
        .combine(MutableStateFlow(exerciseCatalog)) { entries, catalog ->
            entries.toPlanMap(catalog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val presets: StateFlow<List<TrainingPresetUiModel>> = combine(
        presetEntities,
        presetExerciseEntities,
        MutableStateFlow(exerciseCatalog)
    ) { pEntities, peEntities, catalog ->
        buildPresetUiModels(pEntities, peEntities, catalog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun markTodayCompleted(date: LocalDate, isCompleted: Boolean) {
        viewModelScope.launch {
            val dateString = date.toString()
            if (isCompleted) {
                val todaysPlan = calendarPlanRepository.getByDate(dateString)
                val insertedIds = todaysPlan.map { entry ->
                    exerciseLogRepository.addLog(
                        exerciseName = entry.exerciseName,
                        quantity = entry.quantity,
                        metric = entry.metric,
                        date = dateString,
                    )
                }
                completionRepository.setCompleted(dateString, true)
                completionRepository.saveCompletionLogIds(dateString, insertedIds)
            } else {
                val completionIds = completionRepository.getCompletionLogIds(dateString)
                completionRepository.clearCompletion(dateString)
                completionIds.forEach { id -> exerciseLogRepository.deleteById(id) }
            }
        }
    }

    fun observeCompletion(date: String) = completionRepository.observeByDate(date)
    fun observeCompletionLogIds(date: String) = completionRepository.observeCompletionLogIds(date)

    fun savePlan(date: LocalDate, exercises: List<ExerciseEntry>) {
        viewModelScope.launch {
            val dateString = date.toString()
            val entriesToPersist = exercises
                .mapNotNull { entry ->
                    val quantity = entry.validQuantityOrNull() ?: return@mapNotNull null
                    if (entry.name.isBlank() || entry.metric.isBlank()) return@mapNotNull null
                    entry to quantity
                }
            
            if (date.isBefore(LocalDate.now())) {
                exerciseLogRepository.deleteByDate(dateString)
                entriesToPersist.forEach { (entry, quantity) ->
                    exerciseLogRepository.addLog(
                        exerciseName = entry.name,
                        quantity = quantity,
                        metric = entry.metric,
                        date = dateString,
                    )
                }
            } else {
                calendarPlanRepository.replaceDatePlan(
                    date = dateString,
                    entries = entriesToPersist.map { (entry, quantity) ->
                        CalendarPlanInput(
                            exerciseName = entry.name,
                            metric = entry.metric,
                            quantity = quantity,
                        )
                    },
                )
            }
        }
    }

    fun clearPlan(date: LocalDate) {
        viewModelScope.launch {
            val dateString = date.toString()
            if (date.isBefore(LocalDate.now())) {
                exerciseLogRepository.deleteByDate(dateString)
            } else {
                calendarPlanRepository.deleteByDate(dateString)
                exerciseLogRepository.deleteByDate(dateString)
                if (date == LocalDate.now()) {
                    completionRepository.clearCompletion(dateString)
                }
            }
        }
    }

    fun savePreset(
        presetId: String?,
        name: String,
        exercises: List<ExerciseEntry>,
        onSaved: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val input = exercises
                .mapNotNull { exercise ->
                    val quantity = exercise.validQuantityOrNull() ?: return@mapNotNull null
                    if (exercise.name.isBlank() || exercise.metric.isBlank()) return@mapNotNull null
                    TrainingPresetExerciseInput(
                        exerciseName = exercise.name,
                        metric = exercise.metric,
                        quantity = quantity,
                    )
                }
            if (name.isBlank() || input.isEmpty()) {
                onSaved(false)
                return@launch
            }
            if (presetId == null) {
                presetRepository.addPreset(name = name, exercises = input)
            } else {
                presetRepository.updatePreset(presetId = presetId, name = name, exercises = input)
            }
            onSaved(true)
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            presetRepository.deletePreset(presetId)
        }
    }

    private fun loadExerciseCatalog(context: android.content.Context): List<ExerciseCatalogEntry> {
        val loadedCatalog = runCatching {
            val jsonText = context.assets.open("data/home_exercise_catalog.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonText)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(index)
                    val name = item.optString("name").trim()
                    val metric = item.optString("metric").trim()
                    if (name.isNotBlank() && metric.isNotBlank()) {
                        add(ExerciseCatalogEntry(name = name, metric = metric))
                    }
                }
            }
        }.getOrDefault(emptyList())
        return if (loadedCatalog.isNotEmpty()) loadedCatalog else listOf(ExerciseCatalogEntry("Push-ups", "reps"))
    }

    private fun List<ExerciseLogEntity>.toWorkoutMap(
        catalog: List<ExerciseCatalogEntry>,
    ): Map<LocalDate, List<ExerciseEntry>> {
        return this
            .mapNotNull { log ->
                val date = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@mapNotNull null
                date to log.toExerciseEntry(catalog)
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }

    private fun List<CalendarPlanEntryEntity>.toPlanMap(
        catalog: List<ExerciseCatalogEntry>,
    ): Map<LocalDate, List<ExerciseEntry>> {
        return this
            .mapNotNull { entry ->
                val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                val catalogEntry = catalog.firstOrNull { it.name.equals(entry.exerciseName, ignoreCase = true) }
                val metric = if (entry.metric.isNotBlank()) entry.metric else catalogEntry?.metric ?: "units"
                date to ExerciseEntry(
                    name = entry.exerciseName,
                    metric = metric,
                    value = formatMetricValue(entry.quantity),
                )
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }

    private fun buildPresetUiModels(
        presets: List<TrainingPresetEntity>,
        presetExercises: List<TrainingPresetExerciseEntity>,
        catalog: List<ExerciseCatalogEntry>,
    ): List<TrainingPresetUiModel> {
        val exercisesByPresetId = presetExercises
            .groupBy { it.presetId }
            .mapValues { (_, entries) -> entries.sortedBy { it.sortOrder } }
        return presets.map { preset ->
            val exercises = exercisesByPresetId[preset.id].orEmpty().map { exercise ->
                val catalogEntry = catalog.firstOrNull { it.name.equals(exercise.exerciseName, ignoreCase = true) }
                val metric = if (exercise.metric.isNotBlank()) exercise.metric else catalogEntry?.metric ?: "units"
                ExerciseEntry(
                    name = exercise.exerciseName,
                    metric = metric,
                    value = formatMetricValue(exercise.quantity),
                )
            }
            TrainingPresetUiModel(id = preset.id, name = preset.name, exercises = exercises)
        }
    }

    private fun ExerciseLogEntity.toExerciseEntry(
        catalog: List<ExerciseCatalogEntry>,
    ): ExerciseEntry {
        val catalogEntry = catalog.firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }
        val resolvedMetric = when {
            metric.startsWith("sets:") -> "reps"
            metric.isNotBlank() -> metric
            catalogEntry != null -> catalogEntry.metric
            else -> "units"
        }
        return ExerciseEntry(
            name = exerciseName,
            metric = resolvedMetric,
            value = formatMetricValue(quantity),
            sourceLogId = id,
        )
    }

    private fun formatMetricValue(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    }

    private fun ExerciseEntry.validQuantityOrNull(): Double? {
        val parsed = value.toDoubleOrNull() ?: return null
        return if (parsed > 0.0) parsed else null
    }
}

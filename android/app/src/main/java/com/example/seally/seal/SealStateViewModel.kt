package com.example.seally.seal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.seally.data.local.entity.SealHiddenPointDailyEntity
import com.example.seally.data.repository.DailyGoalProgressRepository
import com.example.seally.data.repository.ExerciseLogRepository
import com.example.seally.data.repository.SealHiddenPointRepository
import com.example.seally.data.repository.TargetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val CALORIES_GOAL_NAME = "CALORIES"

class SealStateViewModel(application: Application) : AndroidViewModel(application) {
    private val mTargetRepository = TargetRepository(application)
    private val mDailyGoalProgressRepository = DailyGoalProgressRepository(application)
    private val mExerciseLogRepository = ExerciseLogRepository(application)
    private val mSealHiddenPointRepository = SealHiddenPointRepository(application)

    val visualState: StateFlow<SealVisualState> = mSealHiddenPointRepository
        .observeTotalPoints()
        .map { SealVisualState.fromHiddenPoints(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SealVisualState.fromHiddenPoints(0),
        )

    init {
        observeInputsAndSyncHiddenPoints()
    }

    private fun observeInputsAndSyncHiddenPoints() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                mTargetRepository.observeTargets(),
                mDailyGoalProgressRepository.observeByGoalName(CALORIES_GOAL_NAME),
                mExerciseLogRepository.observeAll(),
            ) { targets, calorieProgress, workouts ->
                Triple(targets, calorieProgress, workouts)
            }.collectLatest { (targets, calorieProgress, workouts) ->
                val calorieTarget = targets.firstOrNull { it.goalName == CALORIES_GOAL_NAME }?.targetValue
                val calorieProgressByDate = calorieProgress.associate { it.date to it.progressValue }
                val workoutDates = workouts.map { it.date }.toSet()
                val startDate = resolveStartDate(
                    calorieProgressDates = calorieProgressByDate.keys,
                    workoutDates = workoutDates,
                )

                if (startDate == null) {
                    mSealHiddenPointRepository.replaceAll(emptyList())
                    return@collectLatest
                }

                val today = LocalDate.now()
                val dailyDeltas = buildDailyDeltas(
                    startDate = startDate,
                    endDateInclusive = today,
                    caloriesTarget = calorieTarget,
                    caloriesByDate = calorieProgressByDate,
                    workoutDates = workoutDates,
                )
                mSealHiddenPointRepository.replaceAll(dailyDeltas)
            }
        }
    }

    private fun resolveStartDate(
        calorieProgressDates: Set<String>,
        workoutDates: Set<String>,
    ): LocalDate? {
        val earliestCalorieDate = calorieProgressDates
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .minOrNull()
        val earliestWorkoutDate = workoutDates
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .minOrNull()

        return listOfNotNull(earliestCalorieDate, earliestWorkoutDate).minOrNull()
    }

    private fun buildDailyDeltas(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        caloriesTarget: Double?,
        caloriesByDate: Map<String, Double>,
        workoutDates: Set<String>,
    ): List<SealHiddenPointDailyEntity> {
        val result = mutableListOf<SealHiddenPointDailyEntity>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDateInclusive)) {
            val dateString = currentDate.toString()
            val calories = caloriesByDate[dateString] ?: 0.0
            val caloriesOverGoalByThousand = caloriesTarget != null && calories - caloriesTarget >= 1000.0
            val hadWorkout = dateString in workoutDates
            val dailyDelta = (if (caloriesOverGoalByThousand) 1 else 0) + (if (hadWorkout) -1 else 0)
            result += SealHiddenPointDailyEntity(
                date = dateString,
                dailyDelta = dailyDelta,
                caloriesOverGoal = caloriesOverGoalByThousand,
                hadWorkout = hadWorkout,
            )
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return SealStateViewModel(app) as T
            }
        }
    }
}

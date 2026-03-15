package com.example.seally.calendar

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate

data class WorkoutEntry(
    val name: String,
    val sets: Int,
    val reps: Int,
)

class WorkoutPlanViewModel : ViewModel() {
    val mWorkoutPlans = mutableStateMapOf<LocalDate, List<WorkoutEntry>>()

    fun setWorkoutPlan(date: LocalDate, entries: List<WorkoutEntry>) {
        mWorkoutPlans[date] = entries
    }

    fun clearWorkoutPlan(date: LocalDate) {
        mWorkoutPlans.remove(date)
    }
}

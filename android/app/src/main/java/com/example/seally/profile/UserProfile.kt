package com.example.seally.profile

data class UserProfile(
    val name: String = "",
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val goalWeightKg: Float? = null,
    val activityType: String = "",
    val workoutDaysPerWeek: Int? = null,
    val journeyGoal: String = "",
    val waterTargetMl: Int? = null,
    val onboardingCompleted: Boolean = false,
)


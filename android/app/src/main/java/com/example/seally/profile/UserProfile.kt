package com.example.seally.profile

data class UserProfile(
    val name: String = "",
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val goalWeightKg: Float? = null,
    val age: Int? = null,
    val gender: String = "",
    val activityType: String = "",
    val workoutDaysPerWeek: Int? = null,
    val waterTargetMl: Int? = null,
    val onboardingCompleted: Boolean = false,
)


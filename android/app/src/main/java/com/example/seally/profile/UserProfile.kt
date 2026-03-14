package com.example.seally.profile

data class UserProfile(
    val name: String = "",
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val goalWeightKg: Float? = null,
)


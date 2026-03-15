package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_log")
data class NutritionLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "water_ml", defaultValue = "0")
    val waterMl: Int = 0,
    @ColumnInfo(name = "calories_kcal", defaultValue = "0")
    val caloriesKcal: Int = 0,
    @ColumnInfo(name = "protein_g", defaultValue = "0")
    val proteinG: Double = 0.0,
    @ColumnInfo(name = "carbs_g", defaultValue = "0")
    val carbsG: Double = 0.0,
    @ColumnInfo(name = "fats_g", defaultValue = "0")
    val fatsG: Double = 0.0,
    @ColumnInfo(name = "sugar_g", defaultValue = "0")
    val sugarG: Double = 0.0,
    @ColumnInfo(name = "fiber_g", defaultValue = "0")
    val fiberG: Double = 0.0,
)

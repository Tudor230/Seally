package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nutrition_food_entry",
    indices = [Index(value = ["date"])],
)
data class NutritionFoodEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "meal")
    val meal: String,
    @ColumnInfo(name = "calories")
    val calories: Int,
    @ColumnInfo(name = "protein")
    val protein: Int,
    @ColumnInfo(name = "carbs")
    val carbs: Int,
    @ColumnInfo(name = "fats")
    val fats: Int,
    @ColumnInfo(name = "sugars")
    val sugars: Int,
    @ColumnInfo(name = "fibers")
    val fibers: Int,
    @ColumnInfo(name = "is_healthy")
    val isHealthy: Boolean,
)

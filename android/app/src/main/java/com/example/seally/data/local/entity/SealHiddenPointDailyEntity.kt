package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seal_hidden_point_daily")
data class SealHiddenPointDailyEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "daily_delta", defaultValue = "0")
    val dailyDelta: Int = 0,
    @ColumnInfo(name = "calories_over_goal", defaultValue = "0")
    val caloriesOverGoal: Boolean = false,
    @ColumnInfo(name = "had_workout", defaultValue = "0")
    val hadWorkout: Boolean = false,
)

package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_log")
data class ExerciseLogEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "metric")
    val metric: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "preset_name")
    val presetName: String? = null,
)

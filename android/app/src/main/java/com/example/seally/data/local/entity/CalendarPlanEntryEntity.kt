package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_plan_entry",
    indices = [Index(value = ["date"])],
)
data class CalendarPlanEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,
    @ColumnInfo(name = "metric")
    val metric: String,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "preset_name")
    val presetName: String? = null,
)

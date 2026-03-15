package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_day_completion")
data class CalendarDayCompletionEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
)

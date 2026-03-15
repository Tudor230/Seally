package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "daily_goal_progress",
    primaryKeys = ["goal_name", "date"],
    foreignKeys = [
        ForeignKey(
            entity = TargetEntity::class,
            parentColumns = ["goal_name"],
            childColumns = ["goal_name"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["goal_name"]),
        Index(value = ["date"]),
    ],
)
data class DailyGoalProgressEntity(
    @ColumnInfo(name = "goal_name")
    val goalName: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "progress_value", defaultValue = "0")
    val progressValue: Double = 0.0,
)

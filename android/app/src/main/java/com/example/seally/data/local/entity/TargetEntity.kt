package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target")
data class TargetEntity(
    @PrimaryKey
    @ColumnInfo(name = "goal_name")
    val goalName: String,
    @ColumnInfo(name = "target_value")
    val targetValue: Double,
)

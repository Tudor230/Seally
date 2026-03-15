package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "calendar_completion_log",
    primaryKeys = ["date", "log_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CalendarDayCompletionEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["log_id"])],
)
data class CalendarCompletionLogEntity(
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "log_id")
    val logId: String,
)

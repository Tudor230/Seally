package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_preset_exercise",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["preset_id"])],
)
data class TrainingPresetExerciseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "preset_id")
    val presetId: String,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,
    @ColumnInfo(name = "metric")
    val metric: String,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

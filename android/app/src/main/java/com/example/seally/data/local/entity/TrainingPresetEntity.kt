package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_preset")
data class TrainingPresetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
)

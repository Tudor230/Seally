package com.example.seally.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "first_name")
    val firstName: String,
    @ColumnInfo(name = "last_name")
    val lastName: String,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,
    @ColumnInfo(name = "height_cm")
    val heightCm: Int,
    @ColumnInfo(name = "age")
    val age: Int,
    @ColumnInfo(name = "activity_level")
    val activityLevel: String,
    @ColumnInfo(name = "exp", defaultValue = "0")
    val exp: Int = 0,
)

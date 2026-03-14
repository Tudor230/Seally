package com.example.seally.xp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xp")
data class XpEntity(
    @PrimaryKey val id: Long = XpRepository.SINGLETON_ID,
    val totalXp: Int,
)


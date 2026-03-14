package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_log WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_log ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ExerciseLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ExerciseLogEntity)
}

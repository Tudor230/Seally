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

    @Query("SELECT * FROM exercise_log WHERE date = :date ORDER BY id DESC")
    suspend fun getByDate(date: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExerciseLogEntity>>

    @Query("SELECT DISTINCT date FROM exercise_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    suspend fun getLoggedDatesBetween(startDate: String, endDate: String): List<String>

    @Query("SELECT * FROM exercise_log ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ExerciseLogEntity>

    @Query("DELETE FROM exercise_log WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ExerciseLogEntity)
}

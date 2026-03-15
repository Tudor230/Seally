package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.DailyGoalProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalProgressDao {
    @Query("SELECT * FROM daily_goal_progress WHERE goal_name = :goalName ORDER BY date")
    fun observeByGoalName(goalName: String): Flow<List<DailyGoalProgressEntity>>

    @Query("SELECT * FROM daily_goal_progress WHERE date = :date ORDER BY goal_name")
    fun observeByDate(date: String): Flow<List<DailyGoalProgressEntity>>

    @Query("SELECT * FROM daily_goal_progress WHERE goal_name = :goalName AND date = :date LIMIT 1")
    suspend fun getByGoalAndDate(goalName: String, date: String): DailyGoalProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyGoalProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<DailyGoalProgressEntity>)
}

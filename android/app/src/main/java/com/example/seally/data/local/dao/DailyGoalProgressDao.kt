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

    @Query("SELECT * FROM daily_goal_progress WHERE goal_name = :goalName ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentByGoalName(goalName: String, limit: Int): List<DailyGoalProgressEntity>

    @Query("SELECT * FROM daily_goal_progress WHERE date = :date ORDER BY goal_name")
    fun observeByDate(date: String): Flow<List<DailyGoalProgressEntity>>

    @Query("SELECT * FROM daily_goal_progress WHERE date = :date ORDER BY goal_name")
    suspend fun getByDate(date: String): List<DailyGoalProgressEntity>

    @Query("SELECT * FROM daily_goal_progress WHERE goal_name = :goalName AND date = :date LIMIT 1")
    suspend fun getByGoalAndDate(goalName: String, date: String): DailyGoalProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyGoalProgressEntity)

    @Query(
        """
        INSERT OR REPLACE INTO daily_goal_progress(goal_name, date, progress_value)
        SELECT :goalName, :date, :progressValue
        WHERE EXISTS (SELECT 1 FROM target WHERE goal_name = :goalName)
        """,
    )
    suspend fun upsertIfTargetExists(goalName: String, date: String, progressValue: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<DailyGoalProgressEntity>)
}

package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetDao {
    @Query("SELECT * FROM target ORDER BY goal_name")
    fun observeTargets(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM target WHERE goal_name = :goalName LIMIT 1")
    suspend fun getByGoalName(goalName: String): TargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(target: TargetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(targets: List<TargetEntity>)

    @Query("DELETE FROM target WHERE goal_name = :goalName")
    suspend fun deleteByGoalName(goalName: String)
}

package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {
    @Query("SELECT * FROM nutrition_log WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<NutritionLogEntity?>

    @Query("SELECT * FROM nutrition_log WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): NutritionLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NutritionLogEntity)
}

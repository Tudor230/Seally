package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionFoodEntryDao {
    @Query("SELECT * FROM nutrition_food_entry WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<NutritionFoodEntryEntity>>

    @Query("SELECT * FROM nutrition_food_entry WHERE date = :date ORDER BY id DESC")
    suspend fun getByDate(date: String): List<NutritionFoodEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NutritionFoodEntryEntity)

    @Query("DELETE FROM nutrition_food_entry WHERE id = :id")
    suspend fun deleteById(id: String)
}

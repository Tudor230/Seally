package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.TrainingPresetEntity
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPresetDao {
    @Query("SELECT * FROM training_preset ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TrainingPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: TrainingPresetEntity)

    @Update
    suspend fun update(preset: TrainingPresetEntity)

    @Query("DELETE FROM training_preset WHERE id = :id")
    suspend fun deleteById(id: String)
}

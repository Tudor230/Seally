package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.TrainingPresetExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPresetExerciseDao {
    @Query("SELECT * FROM training_preset_exercise ORDER BY preset_id, sort_order, id")
    fun observeAll(): Flow<List<TrainingPresetExerciseEntity>>

    @Query("SELECT * FROM training_preset_exercise WHERE preset_id = :presetId ORDER BY sort_order, id")
    suspend fun getByPresetId(presetId: String): List<TrainingPresetExerciseEntity>

    @Query("DELETE FROM training_preset_exercise WHERE preset_id = :presetId")
    suspend fun deleteByPresetId(presetId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TrainingPresetExerciseEntity>)
}

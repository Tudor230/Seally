package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.TrainingPresetEntity
import com.example.seally.data.local.entity.TrainingPresetExerciseEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class TrainingPresetExerciseInput(
    val exerciseName: String,
    val metric: String,
    val quantity: Double,
)

class TrainingPresetRepository(context: Context) {
    private val mPresetDao = AppDatabase.getInstance(context).trainingPresetDao()
    private val mPresetExerciseDao = AppDatabase.getInstance(context).trainingPresetExerciseDao()

    fun observePresets(): Flow<List<TrainingPresetEntity>> = mPresetDao.observeAll()
    fun observePresetExercises(): Flow<List<TrainingPresetExerciseEntity>> = mPresetExerciseDao.observeAll()

    suspend fun getPresetExercises(presetId: String): List<TrainingPresetExerciseEntity> {
        return mPresetExerciseDao.getByPresetId(presetId)
    }

    suspend fun addPreset(
        name: String,
        exercises: List<TrainingPresetExerciseInput>,
    ): String {
        val id = UUID.randomUUID().toString()
        mPresetDao.insert(TrainingPresetEntity(id = id, name = name))
        replacePresetExercises(id, exercises)
        return id
    }

    suspend fun updatePreset(
        presetId: String,
        name: String,
        exercises: List<TrainingPresetExerciseInput>,
    ) {
        mPresetDao.update(TrainingPresetEntity(id = presetId, name = name))
        replacePresetExercises(presetId, exercises)
    }

    suspend fun deletePreset(presetId: String) {
        mPresetDao.deleteById(presetId)
    }

    private suspend fun replacePresetExercises(
        presetId: String,
        exercises: List<TrainingPresetExerciseInput>,
    ) {
        mPresetExerciseDao.deleteByPresetId(presetId)
        if (exercises.isEmpty()) return
        mPresetExerciseDao.insertAll(
            exercises.mapIndexed { index, entry ->
                TrainingPresetExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    presetId = presetId,
                    exerciseName = entry.exerciseName,
                    metric = entry.metric,
                    quantity = entry.quantity,
                    sortOrder = index,
                )
            },
        )
    }
}

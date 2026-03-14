package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExerciseLogRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).exerciseLogDao()

    fun observeByDate(date: String): Flow<List<ExerciseLogEntity>> = mDao.observeByDate(date)

    suspend fun getRecent(limit: Int = 50): List<ExerciseLogEntity> = mDao.getRecent(limit)

    suspend fun addLog(
        exerciseName: String,
        quantity: Double,
        metric: String,
        date: String,
    ): String {
        val id = UUID.randomUUID().toString()
        mDao.insert(
            ExerciseLogEntity(
                id = id,
                exerciseName = exerciseName,
                quantity = quantity,
                metric = metric,
                date = date,
            ),
        )
        return id
    }
}

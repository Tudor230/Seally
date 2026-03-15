package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExerciseLogRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).exerciseLogDao()

    fun observeByDate(date: String): Flow<List<ExerciseLogEntity>> = mDao.observeByDate(date)
    fun observeAll(): Flow<List<ExerciseLogEntity>> = mDao.observeAll()
    suspend fun getByDate(date: String): List<ExerciseLogEntity> = mDao.getByDate(date)
    suspend fun getLoggedDatesBetween(startDate: String, endDate: String): List<String> =
        mDao.getLoggedDatesBetween(startDate = startDate, endDate = endDate)

    suspend fun getRecent(limit: Int = 50): List<ExerciseLogEntity> = mDao.getRecent(limit)
    suspend fun deleteByDate(date: String) = mDao.deleteByDate(date)
    suspend fun deleteById(id: String) = mDao.deleteById(id)

    suspend fun addLog(
        exerciseName: String,
        quantity: Double,
        metric: String,
        date: String,
        presetName: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        mDao.insert(
            ExerciseLogEntity(
                id = id,
                exerciseName = exerciseName,
                quantity = quantity,
                metric = metric,
                date = date,
                presetName = presetName,
            ),
        )
        return id
    }
}

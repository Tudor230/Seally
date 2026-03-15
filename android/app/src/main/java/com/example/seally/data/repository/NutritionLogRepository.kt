package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.NutritionLogEntity
import kotlinx.coroutines.flow.Flow

class NutritionLogRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).nutritionLogDao()

    fun observeByDate(date: String): Flow<NutritionLogEntity?> = mDao.observeByDate(date)

    suspend fun getByDate(date: String): NutritionLogEntity? = mDao.getByDate(date)

    suspend fun upsert(log: NutritionLogEntity) {
        mDao.upsert(log)
    }

    suspend fun addWater(date: String, deltaMl: Int) {
        val existing = mDao.getByDate(date) ?: NutritionLogEntity(date = date)
        mDao.upsert(
            existing.copy(
                waterMl = (existing.waterMl + deltaMl).coerceAtLeast(0),
            ),
        )
    }
}

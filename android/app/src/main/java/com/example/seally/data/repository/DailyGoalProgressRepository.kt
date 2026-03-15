package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.DailyGoalProgressEntity
import kotlinx.coroutines.flow.Flow

class DailyGoalProgressRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).dailyGoalProgressDao()

    fun observeByGoalName(goalName: String): Flow<List<DailyGoalProgressEntity>> {
        return mDao.observeByGoalName(goalName)
    }

    fun observeByDate(date: String): Flow<List<DailyGoalProgressEntity>> {
        return mDao.observeByDate(date)
    }

    suspend fun getByDate(date: String): List<DailyGoalProgressEntity> {
        return mDao.getByDate(date)
    }

    suspend fun getByGoalAndDate(goalName: String, date: String): DailyGoalProgressEntity? {
        return mDao.getByGoalAndDate(goalName, date)
    }

    suspend fun getRecentByGoalName(goalName: String, limit: Int): List<DailyGoalProgressEntity> {
        return mDao.getRecentByGoalName(goalName, limit)
    }

    suspend fun setProgress(goalName: String, date: String, progressValue: Double) {
        mDao.upsert(
            DailyGoalProgressEntity(
                goalName = goalName,
                date = date,
                progressValue = progressValue,
            ),
        )
    }

    suspend fun setProgressIfTargetExists(goalName: String, date: String, progressValue: Double) {
        mDao.upsertIfTargetExists(
            goalName = goalName,
            date = date,
            progressValue = progressValue,
        )
    }
}

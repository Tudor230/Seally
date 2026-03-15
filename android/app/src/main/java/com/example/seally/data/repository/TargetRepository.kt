package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

class TargetRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).targetDao()

    fun observeTargets(): Flow<List<TargetEntity>> = mDao.observeTargets()

    suspend fun getByGoalName(goalName: String): TargetEntity? = mDao.getByGoalName(goalName)

    suspend fun upsertTarget(goalName: String, targetValue: Double) {
        mDao.upsert(TargetEntity(goalName = goalName, targetValue = targetValue))
    }

    suspend fun upsertTargets(targets: List<TargetEntity>) {
        mDao.upsertAll(targets)
    }

    suspend fun deleteByGoalName(goalName: String) {
        mDao.deleteByGoalName(goalName)
    }
}

package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.SealHiddenPointDailyEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SealHiddenPointRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).sealHiddenPointDailyDao()

    fun observeTotalPoints(): Flow<Int> = mDao.observeTotalDelta().map { it ?: 0 }

    suspend fun replaceAll(entries: List<SealHiddenPointDailyEntity>) {
        mDao.replaceAll(entries)
    }
}

package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.CalendarCompletionLogEntity
import com.example.seally.data.local.entity.CalendarDayCompletionEntity
import kotlinx.coroutines.flow.Flow

class CalendarDayCompletionRepository(context: Context) {
    private val mCompletionDao = AppDatabase.getInstance(context).calendarDayCompletionDao()
    private val mCompletionLogDao = AppDatabase.getInstance(context).calendarCompletionLogDao()

    fun observeByDate(date: String): Flow<CalendarDayCompletionEntity?> = mCompletionDao.observeByDate(date)
    fun observeCompletionLogIds(date: String): Flow<List<String>> = mCompletionLogDao.observeLogIdsByDate(date)
    suspend fun getByDate(date: String): CalendarDayCompletionEntity? = mCompletionDao.getByDate(date)

    suspend fun setCompleted(date: String, isCompleted: Boolean) {
        mCompletionDao.upsert(CalendarDayCompletionEntity(date = date, isCompleted = isCompleted))
    }

    suspend fun clearCompletion(date: String) {
        mCompletionDao.deleteByDate(date)
        mCompletionLogDao.deleteByDate(date)
    }

    suspend fun saveCompletionLogIds(date: String, logIds: List<String>) {
        mCompletionLogDao.deleteByDate(date)
        if (logIds.isEmpty()) return
        mCompletionLogDao.insertAll(logIds.map { logId -> CalendarCompletionLogEntity(date = date, logId = logId) })
    }

    suspend fun getCompletionLogIds(date: String): List<String> = mCompletionLogDao.getLogIdsByDate(date)
}

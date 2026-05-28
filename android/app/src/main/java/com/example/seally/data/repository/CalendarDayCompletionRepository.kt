package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.CalendarCompletionLogEntity
import com.example.seally.data.local.entity.CalendarDayCompletionEntity
import kotlinx.coroutines.flow.Flow

// Used in order to mark the day as completed, or clear the completion status by
// accesing data from the db
class CalendarDayCompletionRepository(context: Context) {
    private val mCompletionDao = AppDatabase.getInstance(context).calendarDayCompletionDao()
    private val mCompletionLogDao = AppDatabase.getInstance(context).calendarCompletionLogDao()

    fun observeByDate(date: String): Flow<CalendarDayCompletionEntity?> = mCompletionDao.observeByDate(date)
    fun observeCompletionLogIds(date: String): Flow<List<String>> = mCompletionLogDao.observeLogIdsByDate(date)
    suspend fun getByDate(date: String): CalendarDayCompletionEntity? = mCompletionDao.getByDate(date)

//  Like an asyncrounous function that can pause and resume execution so that the ui keeps
//  on rendering and not freeze while the app waits for data from the database
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

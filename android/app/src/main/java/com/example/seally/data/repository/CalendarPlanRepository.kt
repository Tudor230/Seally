package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.CalendarPlanEntryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class CalendarPlanInput(
    val exerciseName: String,
    val metric: String,
    val quantity: Double,
)

class CalendarPlanRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).calendarPlanEntryDao()

    fun observeAll(): Flow<List<CalendarPlanEntryEntity>> = mDao.observeAll()
    suspend fun getByDate(date: String): List<CalendarPlanEntryEntity> = mDao.getByDate(date)

    suspend fun replaceDatePlan(date: String, entries: List<CalendarPlanInput>) {
        mDao.deleteByDate(date)
        if (entries.isEmpty()) return
        mDao.insertAll(
            entries.mapIndexed { index, entry ->
                CalendarPlanEntryEntity(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    exerciseName = entry.exerciseName,
                    metric = entry.metric,
                    quantity = entry.quantity,
                    sortOrder = index,
                )
            },
        )
    }

    suspend fun deleteByDate(date: String) = mDao.deleteByDate(date)
}

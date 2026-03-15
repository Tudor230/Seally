package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.CalendarCompletionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarCompletionLogDao {
    @Query("SELECT log_id FROM calendar_completion_log WHERE date = :date")
    fun observeLogIdsByDate(date: String): Flow<List<String>>

    @Query("SELECT log_id FROM calendar_completion_log WHERE date = :date")
    suspend fun getLogIdsByDate(date: String): List<String>

    @Query("DELETE FROM calendar_completion_log WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CalendarCompletionLogEntity>)
}

package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.CalendarDayCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDayCompletionDao {
    @Query("SELECT * FROM calendar_day_completion WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<CalendarDayCompletionEntity?>

    @Query("SELECT * FROM calendar_day_completion WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): CalendarDayCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: CalendarDayCompletionEntity)

    @Query("DELETE FROM calendar_day_completion WHERE date = :date")
    suspend fun deleteByDate(date: String)
}

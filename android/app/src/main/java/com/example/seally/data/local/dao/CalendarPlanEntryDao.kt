package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seally.data.local.entity.CalendarPlanEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarPlanEntryDao {
    @Query("SELECT * FROM calendar_plan_entry ORDER BY date DESC, sort_order, id")
    fun observeAll(): Flow<List<CalendarPlanEntryEntity>>

    @Query("SELECT * FROM calendar_plan_entry WHERE date = :date ORDER BY sort_order, id")
    fun observeByDate(date: String): Flow<List<CalendarPlanEntryEntity>>

    @Query("SELECT * FROM calendar_plan_entry WHERE date = :date ORDER BY sort_order, id")
    suspend fun getByDate(date: String): List<CalendarPlanEntryEntity>

    @Query("DELETE FROM calendar_plan_entry WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CalendarPlanEntryEntity>)
}

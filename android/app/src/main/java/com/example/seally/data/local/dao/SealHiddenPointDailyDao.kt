package com.example.seally.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.seally.data.local.entity.SealHiddenPointDailyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SealHiddenPointDailyDao {
    @Query("SELECT COALESCE(SUM(daily_delta), 0) FROM seal_hidden_point_daily")
    fun observeTotalDelta(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<SealHiddenPointDailyEntity>)

    @Query("DELETE FROM seal_hidden_point_daily")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entries: List<SealHiddenPointDailyEntity>) {
        deleteAll()
        if (entries.isNotEmpty()) {
            upsertAll(entries)
        }
    }
}

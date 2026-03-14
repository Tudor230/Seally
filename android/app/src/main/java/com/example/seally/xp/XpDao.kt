package com.example.seally.xp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XpDao {
    @Query("SELECT * FROM xp WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<XpEntity?>

    @Query("SELECT * FROM xp WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): XpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: XpEntity)

    /**
     * Adds delta XP to the singleton row.
     * Note: if the row doesn't exist yet, this updates 0 rows.
     */
    @Query("UPDATE xp SET totalXp = MAX(totalXp + :delta, 0) WHERE id = :id")
    suspend fun addXp(id: Long, delta: Int): Int
}


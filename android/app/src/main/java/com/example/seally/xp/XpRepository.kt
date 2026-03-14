package com.example.seally.xp

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * XP persistence backed by Room.
 *
 * Stores exactly one row (id=1) that holds total XP.
 */
class XpRepository(context: Context) {
    private val db = XpDatabase.getInstance(context)
    private val dao = db.xpDao()

    fun observeTotalXp(): Flow<Int> {
        return dao.observeById(SINGLETON_ID).map { entity -> entity?.totalXp ?: 0 }
    }

    fun observeLevelState(): Flow<XpLevelState> {
        return observeTotalXp().map { total -> XpLeveling.levelState(total) }
    }

    suspend fun addXp(delta: Int) {
        if (delta == 0) return

        val updatedRows = dao.addXp(id = SINGLETON_ID, delta = delta)
        if (updatedRows == 0) {
            // First run: row doesn't exist yet
            dao.upsert(XpEntity(id = SINGLETON_ID, totalXp = delta.coerceAtLeast(0)))
        }
    }

    suspend fun setTotalXp(totalXp: Int) {
        dao.upsert(XpEntity(id = SINGLETON_ID, totalXp = totalXp.coerceAtLeast(0)))
    }

    companion object {
        const val SINGLETON_ID: Long = 1L
    }
}

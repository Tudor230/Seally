package com.example.seally.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.seally.data.local.AppDatabase
import com.example.seally.nutrition.nutritionDataStore
import com.example.seally.xp.XpProjectionRepository
import com.example.seally.xp.XpRepository

private const val HOME_PREFS_NAME = "home_goal_slots"

class AccountResetRepository(
    private val mContext: Context,
) {
    private val mProfileRepository = ProfileRepository(mContext)
    private val mXpRepository = XpRepository(mContext)
    private val mXpProjectionRepository = XpProjectionRepository(mContext)

    suspend fun resetEverything() {
        mProfileRepository.clear()
        AppDatabase.getInstance(mContext).clearAllTables()
        mXpRepository.setTotalXp(0)
        mXpProjectionRepository.setTodayPendingXp(0)
        mContext.nutritionDataStore.edit { it.clear() }
        mContext.getSharedPreferences(HOME_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

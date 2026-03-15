package com.example.seally.xp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.xpProjectionDataStore by preferencesDataStore(name = "xp_projection_state")

class XpProjectionRepository(context: Context) {
    private val mDataStore = context.applicationContext.xpProjectionDataStore
    private val mPendingDateKey = stringPreferencesKey("pending_xp_date")
    private val mPendingDeltaKey = intPreferencesKey("pending_xp_delta")

    fun observeTodayPendingXp(): Flow<Int> {
        return mDataStore.data.map { prefs ->
            val date = prefs[mPendingDateKey]
            if (date == LocalDate.now().toString()) {
                prefs[mPendingDeltaKey] ?: 0
            } else {
                0
            }
        }
    }

    suspend fun setTodayPendingXp(deltaXp: Int) {
        mDataStore.edit { prefs ->
            prefs[mPendingDateKey] = LocalDate.now().toString()
            prefs[mPendingDeltaKey] = deltaXp
        }
    }
}


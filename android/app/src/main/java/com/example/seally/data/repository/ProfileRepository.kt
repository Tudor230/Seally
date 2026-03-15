package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProfileRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).profileDao()

    fun observeProfile(): Flow<ProfileEntity?> = mDao.observeProfile()

    suspend fun getProfile(): ProfileEntity? = mDao.getProfile()

    suspend fun upsertProfile(profile: ProfileEntity) {
        mDao.upsert(profile)
    }

    suspend fun createDefaultIfMissing() {
        if (mDao.getProfile() != null) return
        mDao.upsert(
            ProfileEntity(
                id = UUID.randomUUID().toString(),
                firstName = "",
                lastName = "",
                weightKg = 0.0,
                heightCm = 0,
                age = 0,
                activityLevel = DEFAULT_ACTIVITY_LEVEL,
                exp = 0,
            ),
        )
    }

    companion object {
        const val DEFAULT_ACTIVITY_LEVEL = "NOT_ACTIVE"
    }
}

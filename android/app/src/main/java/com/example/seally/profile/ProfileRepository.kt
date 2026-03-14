package com.example.seally.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "user_profile")

class ProfileRepository(
    private val context: Context,
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val WEIGHT_KG = floatPreferencesKey("weight_kg")
        val GOAL_WEIGHT_KG = floatPreferencesKey("goal_weight_kg")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            heightCm = prefs[Keys.HEIGHT_CM],
            weightKg = prefs[Keys.WEIGHT_KG],
            goalWeightKg = prefs[Keys.GOAL_WEIGHT_KG],
        )
    }

    suspend fun update(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name

            if (profile.heightCm == null) prefs.remove(Keys.HEIGHT_CM) else prefs[Keys.HEIGHT_CM] = profile.heightCm
            if (profile.weightKg == null) prefs.remove(Keys.WEIGHT_KG) else prefs[Keys.WEIGHT_KG] = profile.weightKg
            if (profile.goalWeightKg == null) prefs.remove(Keys.GOAL_WEIGHT_KG) else prefs[Keys.GOAL_WEIGHT_KG] = profile.goalWeightKg
        }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }
}


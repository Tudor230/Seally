package com.example.seally.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val ACTIVITY_TYPE = stringPreferencesKey("activity_type")
        val WORKOUT_DAYS_PER_WEEK = intPreferencesKey("workout_days_per_week")
        val JOURNEY_GOAL = stringPreferencesKey("journey_goal")
        val WATER_TARGET_ML = intPreferencesKey("water_target_ml")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            heightCm = prefs[Keys.HEIGHT_CM],
            weightKg = prefs[Keys.WEIGHT_KG],
            goalWeightKg = prefs[Keys.GOAL_WEIGHT_KG],
            activityType = prefs[Keys.ACTIVITY_TYPE] ?: "",
            workoutDaysPerWeek = prefs[Keys.WORKOUT_DAYS_PER_WEEK],
            journeyGoal = prefs[Keys.JOURNEY_GOAL] ?: "",
            waterTargetMl = prefs[Keys.WATER_TARGET_ML],
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    suspend fun update(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name

            if (profile.heightCm == null) prefs.remove(Keys.HEIGHT_CM) else prefs[Keys.HEIGHT_CM] = profile.heightCm
            if (profile.weightKg == null) prefs.remove(Keys.WEIGHT_KG) else prefs[Keys.WEIGHT_KG] = profile.weightKg
            if (profile.goalWeightKg == null) prefs.remove(Keys.GOAL_WEIGHT_KG) else prefs[Keys.GOAL_WEIGHT_KG] = profile.goalWeightKg
            if (profile.activityType.isBlank()) prefs.remove(Keys.ACTIVITY_TYPE) else prefs[Keys.ACTIVITY_TYPE] = profile.activityType
            if (profile.workoutDaysPerWeek == null) prefs.remove(Keys.WORKOUT_DAYS_PER_WEEK) else prefs[Keys.WORKOUT_DAYS_PER_WEEK] = profile.workoutDaysPerWeek
            if (profile.journeyGoal.isBlank()) prefs.remove(Keys.JOURNEY_GOAL) else prefs[Keys.JOURNEY_GOAL] = profile.journeyGoal
            if (profile.waterTargetMl == null) prefs.remove(Keys.WATER_TARGET_ML) else prefs[Keys.WATER_TARGET_ML] = profile.waterTargetMl
            prefs[Keys.ONBOARDING_COMPLETED] = profile.onboardingCompleted
        }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }
}


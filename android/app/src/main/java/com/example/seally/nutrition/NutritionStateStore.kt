package com.example.seally.nutrition

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** DataStore for persisting nutrition-related preferences. */
val Context.nutritionDataStore by preferencesDataStore(name = "nutrition_state")

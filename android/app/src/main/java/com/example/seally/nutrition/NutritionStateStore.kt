package com.example.seally.nutrition

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.nutritionDataStore by preferencesDataStore(name = "nutrition_state")

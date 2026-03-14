package com.example.seally.xp

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Turns a single SharedPreferences key into a cold Flow that emits on change.
 */
fun SharedPreferences.asFlow(
    key: String,
    defaultValue: Int,
): Flow<Int> = callbackFlow {
    trySend(getInt(key, defaultValue))

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey == key) {
            trySend(getInt(key, defaultValue))
        }
    }

    registerOnSharedPreferenceChangeListener(listener)
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}


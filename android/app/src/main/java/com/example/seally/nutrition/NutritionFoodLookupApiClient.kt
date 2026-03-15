package com.example.seally.nutrition

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val FOOD_LOOKUP_BASE_URL = "https://seally.onrender.com/nutrition/lookup"

data class NutritionLookupMacros(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int,
    val fibers: Int,
    val sugars: Int,
)

class NutritionFoodLookupApiClient {
    suspend fun lookupByName(foodName: String): NutritionLookupMacros = withContext(Dispatchers.IO) {
        val query = foodName.trim()
        require(query.isNotBlank()) { "Food name is required." }

        val endpoint = "$FOOD_LOOKUP_BASE_URL?q=${Uri.encode(query)}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000_000
            readTimeout = 15_000_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException("Food lookup failed ($responseCode).")
            }

            val responseObject = JSONObject(responseBody)
            val macrosObject = responseObject.optJSONObject("macros")
                ?: throw IllegalStateException("No nutrition values were returned.")

            NutritionLookupMacros(
                calories = macrosObject.readRoundedMacro("calories"),
                protein = macrosObject.readRoundedMacro("protein"),
                carbs = macrosObject.readRoundedMacro("carbohydrates"),
                fats = macrosObject.readRoundedMacro("fat"),
                fibers = macrosObject.readRoundedMacro("fiber"),
                sugars = macrosObject.readRoundedMacro("sugar"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.readRoundedMacro(key: String): Int {
        val numericValue = when (val value = opt(key)) {
            is Number -> value.toDouble()
            is String -> value.trim().replace(",", ".").toDoubleOrNull()
            else -> null
        }
        return (numericValue ?: 0.0).roundToInt().coerceAtLeast(0)
    }
}

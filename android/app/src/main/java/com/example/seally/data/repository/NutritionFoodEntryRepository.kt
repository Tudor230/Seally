package com.example.seally.data.repository

import android.content.Context
import com.example.seally.data.local.AppDatabase
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NutritionFoodEntryRepository(context: Context) {
    private val mDao = AppDatabase.getInstance(context).nutritionFoodEntryDao()

    fun observeByDate(date: String): Flow<List<NutritionFoodEntryEntity>> = mDao.observeByDate(date)
    suspend fun getByDate(date: String): List<NutritionFoodEntryEntity> = mDao.getByDate(date)

    suspend fun getByDate(date: String): List<NutritionFoodEntryEntity> = mDao.getByDate(date)

    suspend fun addEntry(
        date: String,
        name: String,
        meal: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fats: Int,
        sugars: Int,
        fibers: Int,
        isHealthy: Boolean,
    ): String {
        val id = UUID.randomUUID().toString()
        mDao.insert(
            NutritionFoodEntryEntity(
                id = id,
                date = date,
                name = name,
                meal = meal,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fats = fats,
                sugars = sugars,
                fibers = fibers,
                isHealthy = isHealthy,
            ),
        )
        return id
    }

    suspend fun removeEntry(id: String) {
        mDao.deleteById(id)
    }
}

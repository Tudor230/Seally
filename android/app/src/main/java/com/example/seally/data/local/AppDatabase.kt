package com.example.seally.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.seally.data.local.dao.DailyGoalProgressDao
import com.example.seally.data.local.dao.ExerciseLogDao
import com.example.seally.data.local.dao.NutritionLogDao
import com.example.seally.data.local.dao.ProfileDao
import com.example.seally.data.local.dao.TargetDao
import com.example.seally.data.local.entity.DailyGoalProgressEntity
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.local.entity.NutritionLogEntity
import com.example.seally.data.local.entity.ProfileEntity
import com.example.seally.data.local.entity.TargetEntity

@Database(
    entities = [
        ProfileEntity::class,
        TargetEntity::class,
        DailyGoalProgressEntity::class,
        ExerciseLogEntity::class,
        NutritionLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun targetDao(): TargetDao
    abstract fun dailyGoalProgressDao(): DailyGoalProgressDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun nutritionLogDao(): NutritionLogDao

    companion object {
        @Volatile
        private var mInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return mInstance ?: synchronized(this) {
                mInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seally_app.db",
                ).build().also { mInstance = it }
            }
        }
    }
}

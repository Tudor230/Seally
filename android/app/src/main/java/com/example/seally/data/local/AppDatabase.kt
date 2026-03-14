package com.example.seally.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.seally.data.local.dao.DailyGoalProgressDao
import com.example.seally.data.local.dao.ExerciseLogDao
import com.example.seally.data.local.dao.NutritionFoodEntryDao
import com.example.seally.data.local.dao.NutritionLogDao
import com.example.seally.data.local.dao.ProfileDao
import com.example.seally.data.local.dao.TargetDao
import com.example.seally.data.local.entity.DailyGoalProgressEntity
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
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
        NutritionFoodEntryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun targetDao(): TargetDao
    abstract fun dailyGoalProgressDao(): DailyGoalProgressDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun nutritionFoodEntryDao(): NutritionFoodEntryDao

    companion object {
        @Volatile
        private var mInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return mInstance ?: synchronized(this) {
                mInstance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seally_app.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { mInstance = it }
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `nutrition_food_entry` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `meal` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `protein` INTEGER NOT NULL,
                        `carbs` INTEGER NOT NULL,
                        `fats` INTEGER NOT NULL,
                        `sugars` INTEGER NOT NULL,
                        `fibers` INTEGER NOT NULL,
                        `is_healthy` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_nutrition_food_entry_date` ON `nutrition_food_entry` (`date`)",
                )
            }
        }
    }
}

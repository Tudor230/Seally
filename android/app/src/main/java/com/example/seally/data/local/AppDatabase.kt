package com.example.seally.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.seally.data.local.dao.DailyGoalProgressDao
import com.example.seally.data.local.dao.ExerciseLogDao
import com.example.seally.data.local.dao.CalendarCompletionLogDao
import com.example.seally.data.local.dao.CalendarDayCompletionDao
import com.example.seally.data.local.dao.CalendarPlanEntryDao
import com.example.seally.data.local.dao.NutritionFoodEntryDao
import com.example.seally.data.local.dao.NutritionLogDao
import com.example.seally.data.local.dao.ProfileDao
import com.example.seally.data.local.dao.SealHiddenPointDailyDao
import com.example.seally.data.local.dao.TargetDao
import com.example.seally.data.local.dao.TrainingPresetDao
import com.example.seally.data.local.dao.TrainingPresetExerciseDao
import com.example.seally.data.local.entity.CalendarCompletionLogEntity
import com.example.seally.data.local.entity.CalendarDayCompletionEntity
import com.example.seally.data.local.entity.CalendarPlanEntryEntity
import com.example.seally.data.local.entity.DailyGoalProgressEntity
import com.example.seally.data.local.entity.ExerciseLogEntity
import com.example.seally.data.local.entity.NutritionFoodEntryEntity
import com.example.seally.data.local.entity.NutritionLogEntity
import com.example.seally.data.local.entity.ProfileEntity
import com.example.seally.data.local.entity.SealHiddenPointDailyEntity
import com.example.seally.data.local.entity.TargetEntity
import com.example.seally.data.local.entity.TrainingPresetEntity
import com.example.seally.data.local.entity.TrainingPresetExerciseEntity

@Database(
    entities = [
        ProfileEntity::class,
        TargetEntity::class,
        DailyGoalProgressEntity::class,
        ExerciseLogEntity::class,
        NutritionLogEntity::class,
        NutritionFoodEntryEntity::class,
        TrainingPresetEntity::class,
        TrainingPresetExerciseEntity::class,
        CalendarPlanEntryEntity::class,
        CalendarDayCompletionEntity::class,
        CalendarCompletionLogEntity::class,
        SealHiddenPointDailyEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun targetDao(): TargetDao
    abstract fun dailyGoalProgressDao(): DailyGoalProgressDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun nutritionFoodEntryDao(): NutritionFoodEntryDao
    abstract fun trainingPresetDao(): TrainingPresetDao
    abstract fun trainingPresetExerciseDao(): TrainingPresetExerciseDao
    abstract fun calendarPlanEntryDao(): CalendarPlanEntryDao
    abstract fun calendarDayCompletionDao(): CalendarDayCompletionDao
    abstract fun calendarCompletionLogDao(): CalendarCompletionLogDao
    abstract fun sealHiddenPointDailyDao(): SealHiddenPointDailyDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `training_preset` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `training_preset_exercise` (
                        `id` TEXT NOT NULL,
                        `preset_id` TEXT NOT NULL,
                        `exercise_name` TEXT NOT NULL,
                        `metric` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`preset_id`) REFERENCES `training_preset`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_training_preset_exercise_preset_id` ON `training_preset_exercise` (`preset_id`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_plan_entry` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `exercise_name` TEXT NOT NULL,
                        `metric` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_plan_entry_date` ON `calendar_plan_entry` (`date`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_day_completion` (
                        `date` TEXT NOT NULL,
                        `is_completed` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_completion_log` (
                        `date` TEXT NOT NULL,
                        `log_id` TEXT NOT NULL,
                        PRIMARY KEY(`date`, `log_id`),
                        FOREIGN KEY(`log_id`) REFERENCES `exercise_log`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(`date`) REFERENCES `calendar_day_completion`(`date`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_completion_log_log_id` ON `calendar_completion_log` (`log_id`)")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `seal_hidden_point_daily` (
                        `date` TEXT NOT NULL,
                        `daily_delta` INTEGER NOT NULL DEFAULT 0,
                        `calories_over_goal` INTEGER NOT NULL DEFAULT 0,
                        `had_workout` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

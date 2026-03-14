package com.example.seally.xp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [XpEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class XpDatabase : RoomDatabase() {
    abstract fun xpDao(): XpDao

    companion object {
        @Volatile
        private var instance: XpDatabase? = null

        fun getInstance(context: Context): XpDatabase {
            return instance ?: synchronized(this) {
                instance
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        XpDatabase::class.java,
                        "seally_xp.db",
                    ).build().also { instance = it }
            }
        }
    }
}


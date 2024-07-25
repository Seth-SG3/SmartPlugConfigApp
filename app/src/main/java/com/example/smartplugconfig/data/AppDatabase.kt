package com.example.smartplugconfig.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PowerReading::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun powerReadingDao(): PowerReadingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext,
                    AppDatabase::class.java, "app_database").build()
            }
    }
}

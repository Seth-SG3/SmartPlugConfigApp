package com.example.smartplugconfig.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PowerReadingDao {
    @Insert
    suspend fun insertReading(powerReading: PowerReading)

    @Query("SELECT * FROM power_readings ORDER BY timestamp DESC")
    suspend fun getAllReadings(): List<PowerReading>
}

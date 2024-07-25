package com.example.smartplugconfig.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "power_readings")
data class PowerReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val powerValue: String
)
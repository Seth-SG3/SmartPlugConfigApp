package com.example.smartplugconfig

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {

    fun saveToCsv(context: Context, data: String) {
        val file = File(context.getExternalFilesDir(null), "power_readings.csv")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            FileWriter(file, true).use { writer ->
                writer.append("$timestamp, $data\n") // Ensure correct newline character
            }
            Log.d("CsvUtils", "Data written to CSV: $timestamp, $data in ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("CsvUtils", "Error writing to CSV", e)
        }
    }
}
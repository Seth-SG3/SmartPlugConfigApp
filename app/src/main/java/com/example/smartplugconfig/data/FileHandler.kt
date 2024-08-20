package com.example.smartplugconfig.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileHandler(private val applicationContext : Context) {



    // Writes to power records text file
    fun writeToFile(data: String, context: Context) {
        val file = File(context.filesDir, "power_records.txt")
        try {
            FileOutputStream(file, true).use { output ->
                output.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(): String {
        val file = File(applicationContext.filesDir, "power_records.txt")
        return if (file.exists()) {
            file.readText()
        } else {
            "File not found"
        }
    }

    //Clears the file on restart
    fun clearFile() {
        val file = File(applicationContext.filesDir, "power_records.txt")
        try {
            FileOutputStream(file).use { output ->
                output.write("".toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
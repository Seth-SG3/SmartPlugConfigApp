package com.example.smartplugconfig.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.smartplugconfig.MainViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PowerRepository(private val context: Context, private val viewModel: MainViewModel) {

    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val powerReadingDao: PowerReadingDao = database.powerReadingDao()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun getPowerReading(): String = withContext(Dispatchers.IO) {
        val deferredResult = CompletableDeferred<String>()

        viewModel.getPowerReading(context) { result ->
            deferredResult.complete(result)
        }

        deferredResult.await()
    }

    suspend fun insertPowerReading(powerReading: PowerReading) {
        powerReadingDao.insertReading(powerReading)
    }

    suspend fun getAllReadings(): List<PowerReading> {
        return powerReadingDao.getAllReadings()
    }
}
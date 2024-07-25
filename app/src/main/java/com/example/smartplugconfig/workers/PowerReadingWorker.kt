package com.example.smartplugconfig.workers


import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartplugconfig.MainViewModel
import com.example.smartplugconfig.data.PowerReading
import com.example.smartplugconfig.data.PowerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PowerReadingWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val viewModel = MainViewModel(Application())
            val repository = PowerRepository(applicationContext, viewModel)

            val powerValue = repository.getPowerReading()
            val powerReading = PowerReading(
                timestamp = System.currentTimeMillis(),
                powerValue = powerValue
            )

            repository.insertPowerReading(powerReading)

            Log.d("PowerReadingWorker", "Power reading logged: $powerValue")

            Result.success()
        } catch (exception: Exception) {
            Log.e("PowerReadingWorker", "Error logging power reading", exception)
            Result.failure()
        }
    }
}
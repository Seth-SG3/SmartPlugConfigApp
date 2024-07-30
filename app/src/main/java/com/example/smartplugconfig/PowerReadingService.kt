package com.example.smartplugconfig

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PowerReadingService : Service() {

    private val handler = Handler()
    private val viewModel = MainViewModel.getInstance()
    private lateinit var wakeLock: PowerManager.WakeLock

    private val runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                ensureIpAddressAndReadPower(applicationContext)
            }
            handler.postDelayed(this, 60000) // Run every 1 minute
            Log.d("PowerReadingService", "Runnable executed")
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        handler.post(runnable)
        acquireWakeLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForegroundService() {
        val channelId = "PowerReadingServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Power Reading Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Power Reading Service")
            .setContentText("Collecting power readings every minute")
            .setSmallIcon(R.drawable.dice_6) // Replace with your app's icon
            .build()

        startForeground(1, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PowerReadingService::WakeLock")
        wakeLock.acquire()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun ensureIpAddressAndReadPower(context: Context) {
        if (viewModel.ipAddress.value == null || viewModel.ipAddress.value == "No devices found") {
            scanForDevicesAndWaitForIp(context) { result ->
                if (result != "No devices found") {
                    CoroutineScope(Dispatchers.IO).launch {
                        readPowerAndSave(context)
                    }
                }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                readPowerAndSave(context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun readPowerAndSave(context: Context) {
        val result = withContext(Dispatchers.IO) {
            var powerReadingResult: String? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            viewModel.getPowerReading(context) { reading ->
                powerReadingResult = reading
                latch.countDown()
            }
            latch.await()
            powerReadingResult ?: "Error: No power reading obtained"
        }
        saveToCsv(context, result)
    }

    private fun saveToCsv(context: Context, data: String) {
        val file = File(context.getExternalFilesDir(null), "power_readings.csv")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            FileWriter(file, true).use { writer ->
                writer.append("$timestamp, $data\n")
                //writer.append(",")
            }
            Log.d("PowerReadingService", "Data written to CSV: $timestamp, $data in ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("PowerReadingService", "Error writing to CSV", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun scanForDevicesAndWaitForIp(context: Context, onScanCompleted: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val hotspotStatus = viewModel.turnOnHotspot(context)
            Log.d("PowerReadingService", hotspotStatus)

            viewModel.scanDevices(context) { result ->
                if (result == "No devices found") {
                    Log.d("PowerReadingService", "No devices found, will retry in 5 seconds")
                    handler.postDelayed({
                        scanForDevicesAndWaitForIp(context, onScanCompleted)
                    }, 5000) // Retry every 60 seconds
                } else {
                    Log.d("PowerReadingService", "Device found: $result")
                    onScanCompleted(result)
                }
            }
        }
    }
}

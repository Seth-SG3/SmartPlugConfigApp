package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import kotlin.coroutines.resume

class PowerReadingService : Service() {


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        acquireWakeLock()
    }

    private val viewModel = MainViewModel.getInstance()
    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleAlarm()
        serviceScope.launch {
            writeData(
                viewModel,
                "4G-UFI-CFE",
                "1234567890",
                "Now"
            )  // Call the periodic task here
        }
            return START_STICKY

    }
    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = "PowerReadingServiceChannel"
        val channel = NotificationChannel(
            channelId,
            "Power Reading Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Power Reading Service")
            .setContentText("Doing something")
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


    fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PowerReadingService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        // Set the alarm to start at approximately 00:00
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.SECOND, 0)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES / 15, // Every minute
            pendingIntent
        )
    }


    suspend fun writeData(viewModel: MainViewModel, ssid: String, password: String, currentTime: String): String {
        return suspendCancellableCoroutine { cont ->
            viewModel.getPowerReading { powerReading ->
                cont.resume(powerReading)
                // Check network connection
                if (powerReading != "ConnectionFailure") {


                    // Write to file
                    val record = "$currentTime - Power: $powerReading\n"
                    writeToFile(record)
                }else{
                    // connectToWifi(ssid = ssid, password = password, state = 3, status = {Unit})
                    writeToFile("$currentTime : Connection Failure\n")
                }
            }
        }
    }

    fun writeToFile(data: String) {
        val file = File(applicationContext.filesDir, "power_records.txt")
        try {
            FileOutputStream(file, true).use { output ->
                output.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(context: MainActivity): String {
        val file = File(context.filesDir, "power_records.txt")
        return if (file.exists()) {
            file.readText()
        } else {
            "File not found"
        }
    }

    private fun clearFile(context: MainActivity) {
        val file = File(context.filesDir, "power_records.txt")
        try {
            FileOutputStream(file).use { output ->
                output.write("".toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
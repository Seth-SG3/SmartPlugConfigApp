package com.example.smartplugconfig

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PowerReadingService : Service() {

    private val viewModel = MainViewModel.getInstance()
    private lateinit var wakeLock: PowerManager.WakeLock
    var lastReceivedTime: Long = System.currentTimeMillis()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        ServiceHolder.setService(this)
        startForegroundService()
        viewModel.setupMQTTBroker(applicationContext)
        checkAndEnableHotspot(applicationContext,viewModel)
        reconfigMqtt(viewModel)
        acquireWakeLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceHolder.clearService()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

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
            .setContentText("Setting up MQTT broker")
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
    private fun checkAndEnableHotspot(context: Context, viewModel: MainViewModel) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    if (!viewModel.isLocalOnlyHotspotEnabled()) {
                        Log.d("Hotspot", "Hotspot is off, turning it on...")
                        viewModel.turnOnHotspot(context)
                    }
                    delay(60000) // Check every minute
                }
            }
        }
    }

    private fun reconfigMqtt(viewModel: MainViewModel){
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    val currentTime = System.currentTimeMillis()
                    Log.d("MQTT", "current time : $currentTime, last received: $lastReceivedTime")
                    if (currentTime - lastReceivedTime > 5 * 60 * 1000) {
                        viewModel.sendMQTTConfig { result ->
                            viewModel.setCurrentTextOutput(result)
                        }
                        viewModel.scanDevices { result ->
                            viewModel.setCurrentTextOutput(result)
                            result.let { ip -> viewModel.setIpAddress(ip) } // Set the IP address in the ViewModel.
                        }
                    }
                    delay(60000)
                }
            }
        }

    }

}

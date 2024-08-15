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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import getPhoneMacAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import restartMiFiDongle
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.coroutines.resume

class PowerReadingService : Service() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {   // Triggered once on class creation
        super.onCreate()
        startForegroundService()
        acquireWakeLock()
        clearFile()
        getPhoneMacAddress()
    }
    private var counter by mutableIntStateOf(1)
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

    // Every time this is called
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        counter += 1
        scheduleAlarm() // Set to restart after a minute
        Log.d("counter", counter.toString())
        if (counter % 1440 != 0) {
            val activityIntent = Intent(this, DataCycleActivity::class.java)
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(activityIntent)
            return START_STICKY
        } else {
            restartMiFiDongle()
            Log.d("PowerReading", "Restarting MiFi device")
            return START_STICKY
        }
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

    //Returns current time and date as a string
    private fun getCurrentTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        return currentDateTime.format(formatter)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PowerReadingService::WakeLock"
        )
        wakeLock.acquire(10*60*1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }


    private fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PowerReadingService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


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

    // Gets data and then writes to a file
    private suspend fun writeData(
        viewModel: MainViewModel,
        ssid: String,
        password: String,
        currentTime: String,
        context: Context,
        connectivityManagerProvider : ConnectivityManagerProvider
    ): String {
        return suspendCancellableCoroutine { cont ->
            viewModel.getPowerReading { powerReading -> // Gets the current power
                cont.resume(powerReading)
                // Check network connection
                if (powerReading.contains("Watts", ignoreCase = true)) {
                    // Write power to file
                    Log.d("PowerReading", "Power value is suitable")
                    val record = "$currentTime - Power: $powerReading\n"
                    writeToFile(record, context = context)
                } else {
                    Log.d("PowerReading", "Connection has failed")


                        connectToWifi(
                            ssid = ssid,
                            password = password,
                            connectivityManagerProvider = connectivityManagerProvider
                        ) // If connection fails it reconnects
                        writeToFile("$currentTime : Connection Failure\n", context = context)
                    }
                }
            }
        }


    // Writes to power records text file
    private fun writeToFile(data: String, context: Context) {
        val file = File(context.filesDir, "power_records.txt")
        try {
            FileOutputStream(file, true).use { output ->
                output.write(data.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //TODO: Implement this as well as sorting out UI for this setting
    fun readFromFile(context: MainActivity): String {
        val file = File(context.filesDir, "power_records.txt")
        return if (file.exists()) {
            file.readText()
        } else {
            "File not found"
        }
    }

    //Clears the file on restart
    private fun clearFile() {
        val file = File(applicationContext.filesDir, "power_records.txt")
        try {
            FileOutputStream(file).use { output ->
                output.write("".toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Connect to normal wifi
    @SuppressLint("ServiceCast")
    fun connectToWifi(ssid: String, password: String, connectivityManagerProvider : ConnectivityManagerProvider) {

        val connectivityManager = connectivityManagerProvider.getConnectivityManager()

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        connectivityManager.requestNetwork(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    Log.d("WifiConnection", "Connected to $ssid")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiConnection", "Connection to $ssid failed")
                }
            })

    }


    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    fun DataCycleView(connectivityManagerProvider : ConnectivityManagerProvider) {
        var power by remember { mutableStateOf("Initialising") }
        val context = LocalContext.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF00B140))
            ) {
                LaunchedEffect(Unit) {
                    serviceScope.launch {
                        power =
                            writeData(
                                viewModel,
                                "4G-UFI-CFE",   // TODO: Make this not hard coded
                                "1234567890",
                                getCurrentTime(),
                                context = context,
                                connectivityManagerProvider
                            )
                    }
                }
                Text(text = power)
                Text(text = getCurrentTime())
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        restartMiFiDongle()

                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Set button color
                ) {
                    Text("Restart Mi-Fi", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))

            }
        }
    }
}

class DataCycleActivity : ComponentActivity() {
    private lateinit var connectivityManagerProvider: ConnectivityManagerProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        connectivityManagerProvider = ConnectivityManagerProvider(applicationContext)

        super.onCreate(savedInstanceState)
        setContent {
            val service = PowerReadingService()
            service.DataCycleView(connectivityManagerProvider)
        }
    }
}

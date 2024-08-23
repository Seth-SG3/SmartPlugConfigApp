package com.example.smartplugconfig


import kotlinx.coroutines.asCoroutineDispatcher
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Binder
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
import com.example.smartplugconfig.data.FileHandler
import com.example.smartplugconfig.data.WifiManagerProvider.connectivityManager
import com.example.smartplugconfig.data.getPhoneMacAddress
import com.example.smartplugconfig.data.restartMiFiDongle
import com.google.firebase.firestore.util.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

class PowerReadingService : Service() {

    private var counter by mutableIntStateOf(1)
    private val viewModel = MainViewModel.getInstance()
    private lateinit var wakeLock: PowerManager.WakeLock
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var fileHandler: FileHandler
    private val onCreateLatch = CountDownLatch(1)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {   // Triggered once on class creation

        super.onCreate()
        acquireWakeLock()
        fileHandlerInitialise()
        fileHandler.clearFile()


        getPhoneMacAddress()
        setupMqttBroker(applicationContext)
        checkAndEnableHotspot(applicationContext,viewModel)
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PowerReadingService = this@PowerReadingService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // Every time this is called
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        counter += 1
        scheduleAlarm() // Set to restart after a minute
        Log.d("counter", counter.toString())
        if(!::fileHandler.isInitialized) fileHandlerInitialise()
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

    private fun fileHandlerInitialise() {
        fileHandler = FileHandler(applicationContext)
        onCreateLatch.countDown()
        Log.d(this.toString(), "countdown")
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
    ): String {
        // Wait until initialization is complete

        withContext(Dispatchers.IO) {
            onCreateLatch.await()
        }
        return suspendCancellableCoroutine { cont ->
            viewModel.getPowerReading(object : PowerReadingCallback {
                override fun onPowerReadingReceived(power: String) {
                }
                }) { powerReading -> // Gets the current power
                cont.resume(powerReading)
                // Check network connection
                if (powerReading.contains("Watts", ignoreCase = true)) {
                    // Write power to file
                    Log.d("PowerReading", "Power value is suitable")
                    val record = "$currentTime - Power: $powerReading\n"
                    Log.d("PowerReading", record)
                    fileHandler.writeToFile(record, context = context)
                } else {
                    Log.d("PowerReading", "Connection has failed")


                        connectToWifi(
                            ssid = ssid,
                            password = password,
                        ) // If connection fails it reconnects
                    fileHandler.writeToFile("$currentTime : Connection Failure\n", context = context)
                    }
                }

            }
        }
    // Connect to normal wifi
    @SuppressLint("ServiceCast")
    fun connectToWifi(ssid: String, password: String) {


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
    fun DataCycleView() {
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
                Button(
                    onClick = {
                        fileHandler.readFromFile()

                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Set button color
                ) {
                    Text("Show power values", color = Color.White)
                }

            }
        }
    }
}

class DataCycleActivity : ComponentActivity() {

    private var service: PowerReadingService? = null
    private var bound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PowerReadingService.LocalBinder
            this@DataCycleActivity.service = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindToService()
        setContent {
            if (bound) {
                service?.DataCycleView()
            } else {
                // Display a loading or placeholder UI while the service is binding
                Text("Loading...")
            }
        }
    }

    private fun bindToService() {
        Intent(this, PowerReadingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from the service
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
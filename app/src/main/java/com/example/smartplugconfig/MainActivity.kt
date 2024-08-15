package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import kotlinx.coroutines.delay
import java.util.Calendar


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = arrayOf(     // Any required permissions
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        initialisation()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val intent = Intent(this, PowerReadingService::class.java)
        startForegroundService(intent)

        scheduleAlarm()

        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp()
            }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
            }
        }


    private fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PowerReadingService::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initialisation() {
        checkAndRequestPermissions()

    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        } else {
            Toast.makeText(this, "All permissions already granted!", Toast.LENGTH_SHORT).show()
            // Return back to code
        }
    }

    // Establishes all necessary permissions for a wifi scan
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                // Return back to code

            } else {
                Toast.makeText(
                    this,
                    "All permissions are required to start the hotspot",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }




@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SmartPlugConfigApp(viewModel: MainViewModel = viewModel()) {
    var currentTextOutput by remember { mutableStateOf("output") }
    val context = LocalContext.current

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,
        setCurrentTextOutput = { currentTextOutput = it },
        context = context,
        viewModel = viewModel,
    )
}


@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val ipsosBlue = Color(0xFF0033A0) // Ipsos Blue color
    val ipsosGreen = Color(0xFF00B140) // Ipsos Green color
    var isScanning by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Scanning") }
    var isHotspotEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isHotspotEnabled = viewModel.isLocalOnlyHotspotEnabled()
            delay(6000) // Check every minute
        }
    }

    // LaunchedEffect to animate the loading text
    LaunchedEffect(isScanning) {
        while (isScanning) {
            loadingText = "Scanning"
            delay(500)
            loadingText = "Scanning."
            delay(500)
            loadingText = "Scanning.."
            delay(500)
            loadingText = "Scanning..."
            delay(500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            //.padding(16.dp)
            .background(ipsosGreen), // Set green background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Icon(
            imageVector = if (isHotspotEnabled) Icons.Default.SignalWifi4Bar else Icons.Default.SignalWifiOff,
            contentDescription = null,
            tint = if (isHotspotEnabled) Color.Green else Color.Red,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = if (isHotspotEnabled) "Hotspot is ON" else "Hotspot is OFF",
            fontSize = 20.sp,
            color = if (isHotspotEnabled) Color.Green else Color.Red
        )
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = {
                val result = viewModel.connectToPlugWifi(context)
                setCurrentTextOutput(result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
        ) {
            Text("Connect to plug", color = Color.White) // Set text color to white for contrast
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.sendWifiConfig { result ->
                    setCurrentTextOutput(result)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Send Wifi config", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                val result = viewModel.turnOnHotspot(context)
                setCurrentTextOutput(result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Switch on Hotspot", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                isScanning = true
                viewModel.scanDevices(context) { result ->
                    isScanning = false
                    setCurrentTextOutput(result)
                    result.let { ip -> viewModel.setIpAddress(ip) } // Set the IP address in the ViewModel.
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Find IP address of plug", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.sendMQTTConfig { result ->
                    setCurrentTextOutput(result)
                }

            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Send MQTT config", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                setupMqttBroker(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("setup mqtt broker", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.findPlugMacAddress{ result ->
                    setCurrentTextOutput(result)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Send MQTT client", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.getPowerReading(context,object : PowerReadingCallback {
                    override fun onPowerReadingReceived(power: String) {
                        setCurrentTextOutput(power)
                    }
                }) { result ->
                    setCurrentTextOutput(result)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Pull power data", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isScanning) loadingText else textToDisplay,
            fontSize = 20.sp, // Increase text size
            fontWeight = FontWeight.Bold, // Make text bold
            color = Color.Black // Text color
        )
    }
}













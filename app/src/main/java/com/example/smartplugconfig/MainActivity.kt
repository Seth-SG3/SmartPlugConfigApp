package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smartplugconfig.data.PowerReading
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import com.example.smartplugconfig.workers.PowerReadingWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = arrayOf(     // Any required permissions
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )

    private val myViewModel: MainViewModel by viewModels()
    lateinit var wifiManager: WifiManager
    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialisation()

        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = this)
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
        val workRequest = PeriodicWorkRequestBuilder<PowerReadingWorker>(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        //val textView: TextView = findViewById(R.id.textView)
        lifecycleScope.launch {
            val readings: List<PowerReading> = myViewModel.getAllReadings()
            //textView.text = readings.joinToString("\n") { "${it.timestamp}: ${it.powerValue}" }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initialisation() {
        wifiManagerInitialisation()
        checkAndRequestPermissions()

    }

    private fun wifiManagerInitialisation(){
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
fun SmartPlugConfigApp(viewModel: MainViewModel = viewModel(), activity: MainActivity) {
    var currentTextOutput by remember { mutableStateOf("output") }
    val context = LocalContext.current

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,
        setCurrentTextOutput = { currentTextOutput = it },
        context = context,
        viewModel = viewModel,
        activity = activity,
    )
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    activity: MainActivity,
) {
    val ipsosBlue = Color(0xFF0033A0) // Ipsos Blue color
    val ipsosGreen = Color(0xFF00B140) // Ipsos Green color
    var isScanning by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Scanning") }

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
        Spacer(modifier = Modifier.height(250.dp))
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
                    if (result != null) {
                        setCurrentTextOutput(result)
                    }
                    result?.let { ip -> viewModel.setIpAddress(ip) } // Set the IP address in the ViewModel.
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
                viewModel.getPowerReading(context) { result ->
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













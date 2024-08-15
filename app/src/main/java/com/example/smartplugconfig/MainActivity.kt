package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import getPlugMacAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        // Any required permissions
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
    )
    lateinit var wifiManager: WifiManager
    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()
    lateinit var connectivityManager: ConnectivityManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialisation()

        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = this, plugWifiNetworks = plugWifiNetworks)
            }
        }
    }

    private fun initialisation() {
        wifiManagerInitialisation()
        checkAndRequestPermissions()

    }

    private fun wifiManagerInitialisation() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @SuppressLint("BatteryLife")
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
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

    // Connect to open wifi (no password) temporary
    fun connectToOpenWifi(ssid: String, status: (Int) -> Unit, state: Int) {
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
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
                    connectionSuccessful(ssid = ssid, status = status, state = state)
                    getPlugMacAddress()
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiConnection", "Connection to $ssid failed")
                    connectionFailed(ssid = ssid, status = status, state = state)
                }

            })
    }

    // Connect to normal wifi
    @SuppressLint("ServiceCast")
    fun connectToWifi(ssid: String, password: String, status: (Int) -> Unit, state: Int) {

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
                    connectionSuccessful(ssid = ssid, status = status, state = state)

                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiConnection", "Connection to $ssid failed")
                    connectionFailed(ssid = ssid, status = status, state = state)
                }
            })

    }

    fun connectionSuccessful(ssid: String, status: (Int) -> Unit, state: Int) {
        Toast.makeText(
            this,
            "Connected to Wifi $ssid",
            Toast.LENGTH_SHORT
        ).show()
        status(state + 2)
    }

    fun connectionFailed(ssid: String, status: (Int) -> Unit, state: Int) {
        Toast.makeText(
            this,
            "Connection to WiFi failed, please retry $ssid",
            Toast.LENGTH_LONG
        ).show()
        status(state + 1)
    }

    private fun getCurrentTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        return currentDateTime.format(formatter)
    }

    @Composable
    fun DataCycle(viewModel: MainViewModel, ssid: String, password: String) {
        // Start the PowerReadingService

        val intent = Intent(this, PowerReadingService::class.java)
        startService(intent)
    }
    /*        var power by remember { mutableStateOf("Place") }
        var currentTime by remember { mutableStateOf(getCurrentTime()) }
        LaunchedEffect(Unit){
            getPhoneMacAddress()
        }
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
            ){
            Text(text = power)
            Text(text = currentTime)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    restartMiFiDongle()

                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Set button color
                ) {
                    Text("Restart Mi-Fi", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    setContentView(R.layout.activity_main)
                    val file = readFromFile(this@MainActivity)
                    Log.d("file", "$file")
                    val textView: TextView = findViewById(R.id.textView)
                    textView.text = file
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue) // Set button color
                ) {
                    Text("Read power values", color = Color.White)
                }
        }
        }

        LaunchedEffect(Unit) {
            var counter = 0
            Log.d("DataCycle", "Starting data cycle")
            while (true) {
                currentTime = getCurrentTime()
                power = writeData(viewModel, context = this@MainActivity, ssid = ssid, password = password, currentTime = currentTime)
                counter += 1
                delay(15000) // 15 seconds delay
                if (counter % (3*60*60/15) == 0){
                    restartMiFiDongle()
                    delay(100*1000) //delay to give enough time for it to reset
                }
            }
        }
    }*/


}

@Composable
fun SmartPlugConfigApp(viewModel: MainViewModel = viewModel(), activity: MainActivity, plugWifiNetworks: SnapshotStateList<String>) {
    var currentTextOutput by remember { mutableStateOf("output") }
    val context = LocalContext.current

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,
        setCurrentTextOutput = { currentTextOutput = it },
        context = context,
        viewModel = viewModel,
        activity = activity,
        plugWifiNetworks = plugWifiNetworks
    )
}
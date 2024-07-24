package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import android.net.wifi.SoftApConfiguration
import androidx.annotation.RequiresApi
import com.example.smartplugconfig.hotspot.UnhiddenSoftApConfigurationBuilder
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(     // Any required permissions
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
    )
    lateinit var wifiManager: WifiManager
    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        initialisation()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = this, plugWifiNetworks = plugWifiNetworks)
            }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
            }
        }

    private fun initialisation() {
        wifiManagerInitialisation()
        checkAndRequestPermissions()

    }

    private fun wifiManagerInitialisation(){
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

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

    // Connect to open wifi (no password) temporary
    fun connectToOpenWifi(ssid: String, status: (Int) -> Unit, state : Int) {
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
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

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
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

    fun connectionSuccessful(ssid : String, status: (Int) -> Unit, state : Int){
        Toast.makeText(
            this,
            "Connected to Wifi $ssid",
            Toast.LENGTH_SHORT
        ).show()
        status(state+2)
    }
    fun connectionFailed(ssid : String, status: (Int) -> Unit, state : Int){
        Toast.makeText(
            this,
            "Connection to WiFi failed, please retry $ssid",
            Toast.LENGTH_LONG
        ).show()
        status(state+1)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
    }
}

class MainViewModel : ViewModel() {
    private val _ipAddress = mutableStateOf<String?>(null)
    val ipAddress: State<String?> = _ipAddress

    fun setIpAddress(ip: String) {
        _ipAddress.value = ip
    }

    fun scanDevices(context: Context, onScanCompleted: (String?) -> Unit) {
        val deviceScanner = DeviceScanner(context)
        deviceScanner.scanDevices(object : DeviceScanner.ScanCallback {
            override fun onScanCompleted(devices: List<String>) {
                val result = if (devices.isEmpty()) {
                    "No devices found"
                } else {
                    devices.joinToString("\\n")
                }
                _ipAddress.value = result
                onScanCompleted(result)
            }
        })
    }


    @Composable
    fun connectToPlugWifi(activity: MainActivity, plugWifiNetworks: SnapshotStateList<String>, status: (Int) -> Unit, state: Int): String {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton(activity = activity, status = status)
            activity.DisplayPlugNetworks(activity, plugWifiNetworks, status = status, state = state)
            ReturnWifiButton(status = status)
        }
        return "Trying to connect to wifi"
    }

    @Composable
    fun ChooseMifiNetwork(activity: MainActivity, status: (Int) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshMifiButton(activity = activity, status = status)
            activity.DisplayMifiNetworks(activity, status = status)
            ReturnWifiButton(status = status)
        }
    }

    fun connectPlugToMifi(activity: MainActivity, status: (Int) -> Unit, ssid: String, password: String) {
        if (activity.mifiNetworks.size == 1) {
            this.sendWifiConfig(ssid, password){
                result -> if (result.contains("error", ignoreCase = true)){
                    Log.e("Error", "Couldn't connect plug to MiFi")
                    status(1)
                }else{
                    Log.d("Success", "Plug and MiFi are connected")
                    status(6)
            }
            }
        }else{
            Log.e("Error", "Length of MiFi should be one")
        }
    }

    fun sendWifiConfig( ssid: String = "Pixel", password: String = "intrasonics",onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = sendWifiConfigInternal(ssid, password)
            onResult(result)
        }
    }

    private suspend fun sendWifiConfigInternal(ssid: String, password: String): String {
        //uses default ip for tasmota plug wifi ap
        val urlString = "http://192.168.4.1/cm?cmnd=Backlog%20SSID1%20${ssid}%3B%20Password1%20${password}%3B%20WifiConfig%205%3B%20restart%201"
        return try {
            Log.d("sendWifiConfig", "Attempting to send request to $urlString")
            val url = URL(urlString)
            withContext(Dispatchers.IO) {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    Log.d("sendWifiConfig", "Request method set to $requestMethod")

                    val responseCode = responseCode
                    Log.d("sendWifiConfig", "Response code: $responseCode")
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = inputStream.bufferedReader().use(BufferedReader::readText)
                        Log.d("sendWifiConfig", "Response: $response")
                        "Response: $response"
                    } else {
                        "HTTP error code: $responseCode"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("sendWifiConfig", "Exception occurred", e)
            "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
        }
    }
    @RequiresApi(33)
    fun turnOnHotspot(context: Context): String {
        // TODO:
        //  1. Investigate if we could use an Andorid API to turn on Local only hotspot automatically
        // 2. Investigate if the ssid and password above can be set to the hotspot
        // 3. Investigate if we can reliably restart the hotspot with the same ssid and password
        startLocalOnlyHotspotWithConfig(
            context = context,
            config = UnhiddenSoftApConfigurationBuilder()
                .setSsid("students-rock")
                .setAutoshutdownEnabled(false)
                .setPassphrase(passphrase="very-secure", securityType=SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
                .build(),
            executor = null,
            callback = object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(result: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(result)
                    Log.d("Hotspot", "Hotspot started")
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.d("Hotspot", "Hotspot stopped")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    Log.d("Hotspot", "Hotspot failed")
                }
            })

        return "starting a newhotspot connection..."
    }
    fun startLocalOnlyHotspotWithConfig(
        context: Context,
        config: SoftApConfiguration,
        executor: Executor?,
        callback: WifiManager.LocalOnlyHotspotCallback
    ) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        WifiManager::class.java.getMethod(
            "startLocalOnlyHotspot", SoftApConfiguration::class.java, Executor::class.java,
            WifiManager.LocalOnlyHotspotCallback::class.java,
        ).invoke(wifiManager, config, executor, callback)
    }

    fun ipScan(): String {
        return "Scanning for IP Address..."
    }

    fun sendMQTTConfig(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = sendMQTTConfigInternal()
            onResult(result)
        }
    }

    private suspend fun sendMQTTConfigInternal(): String {
        val ip = _ipAddress.value
        val urlString = "http://${ip}/cm?cmnd=Backlog%20MqttHost%20testHost%3B%20MqttUser%20Test1%3B%20MqttPassword%20Test2%3B%20Topic%20smartPlugTest"
        return try {
            Log.d("sendMQTTConfig", "Attempting to send request to $urlString")
            val url = URL(urlString)
            withContext(Dispatchers.IO) {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    Log.d("sendMQTTConfig", "Request method set to $requestMethod")

                    val responseCode = responseCode
                    Log.d("sendMQTTConfig", "Response code: $responseCode")
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = inputStream.bufferedReader().use(BufferedReader::readText)
                        Log.d("sendMQTTConfig", "Response: $response")
                        "Response: $response"
                    } else {
                        "HTTP error code: $responseCode"
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
            Log.e("sendMQTTConfig", errorMessage, e)
            errorMessage
        }
    }

    fun getPowerReading(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = getPowerReadingInternal()
            onResult(result)
        }
    }

    private suspend fun getPowerReadingInternal(): String {
        val ip = _ipAddress.value
        val urlString = "http://${ip}/cm?cmnd=Status%208"
        return try {
            Log.d("getPowerReading", "Attempting to send request to $urlString")
            val url = URL(urlString)
            withContext(Dispatchers.IO) {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    Log.d("getPowerReading", "Request method set to $requestMethod")

                    val responseCode = responseCode
                    Log.d("getPowerReading", "Response code: $responseCode")
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = inputStream.bufferedReader().use(BufferedReader::readText)
                        Log.d("getPowerReading", "Response: $response")

                        // Parse the JSON response
                        val jsonObject = JSONObject(response)
                        val statusSNS = jsonObject.getJSONObject("StatusSNS")
                        val energy = statusSNS.getJSONObject("ENERGY")
                        val power = energy.getInt("Power")

                        // Return the formatted string
                        "Power: $power Watts"
                    } else {
                        "HTTP error code: $responseCode"
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
            Log.e("getPowerReading", errorMessage, e)
            errorMessage
        }
    }
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


@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    activity: MainActivity,
    plugWifiNetworks: SnapshotStateList<String>
) {
    val coroutineScope = rememberCoroutineScope()
    var status by remember { mutableIntStateOf(1) }
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
    when (status) {
        1 -> {  //Default gives you all expected options


            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .background(ipsosGreen), // Set green background
                horizontalAlignment = Alignment.CenterHorizontally
                
            ) {

                Spacer(modifier = Modifier.height(250.dp))
                Button(onClick = {
                    Log.d("hi - should be 1", status.toString())
                    activity.updateWifiScan()
                    status = 2
                    Log.d("bye - should be 2", status.toString())
                },
                    colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
                ) {
                    Text("Connect to Plug", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    viewModel.sendWifiConfig { result ->
                        setCurrentTextOutput(result)
                    }
                },            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
                ) {
                    Text("Send Wifi config", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    val result = viewModel.turnOnHotspot(context)
                    setCurrentTextOutput(result)
                },
                    colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
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
                    colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
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
                    viewModel.getPowerReading { result ->
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


        2 -> {      // Allow connections to the plug wifi
            viewModel.connectToPlugWifi(
                activity = activity,
                plugWifiNetworks = plugWifiNetworks,
                status = { status = it },
                state = status
            )
        }
        3 -> {
            status = 2
        }
        4 -> {
            // Choose MiFi Network
           viewModel.ChooseMifiNetwork(
               activity = activity,
               status = {status = it})

           }
        5 -> {
            // Send plug the mifi details


            Text(
                text = "Connecting plug to MiFi Device",
                fontSize = 20.sp, // Increase text size
                fontWeight = FontWeight.Bold, // Make text bold
                color = Color.Black // Text color
            )
            val mifiSsid = activity.mifiNetworks.single()
            viewModel.connectPlugToMifi(
                activity = activity,
                status =  {status = it},
                ssid = mifiSsid,
                password = "1234567890"
            )


        }
        6 -> {
            // Connect to mifi device
            Text(
                text = "Connecting phone to MiFi device",
                fontSize = 20.sp, // Increase text size
                fontWeight = FontWeight.Bold, // Make text bold
                color = Color.Black // Text color
            )
            val mifiSsid = activity.mifiNetworks.single()

            activity.connectToWifi(ssid = mifiSsid, password = "1234567890", status = {status = it}, state = status)
            status = 1

        }
        7-> {
            status = 4
        }
        8->{
            status = 1
        }


        else -> {}
    }
}


// code from facto, class used in ip scan functionality


class DeviceScanner(private val context: Context) {

    fun scanDevices(callback: ScanCallback?) {
        ScanTask(callback).execute()
    }

    @SuppressLint("StaticFieldLeak")
    inner class ScanTask(private val callback: ScanCallback?) : AsyncTask<Void, Void, List<String>>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<String> {
            val deviceList = mutableListOf<String>()
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            Log.d("DeviceScanner", "Starting scan in range 192.168.y.z")

            // Scan the range 192.168.y.z where y and z vary from 0 to 255
                for (z in 2..254) { // Skipping 0 and 255 for z as they are typically not used for hosts
                    val hostAddress = "192.168.100.$z"

                    // Log each host address being scanned
                    Log.d("DeviceScanner", "Scanning IP: $hostAddress")

                    // Check if the specific IP address is being scanned
                    //if (hostAddress == "192.168.240.238") {
                        //Log.d("DeviceScanner", "Specific IP 192.168.240.238 is being scanned")
                    //}

                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(hostAddress, 80), 40) // Increased timeout to 20ms too little think 40ms is best
                        deviceList.add(hostAddress)
                        socket.close()
                    } catch (e: IOException) {
                        Log.d("DeviceScanner", "Failed to connect to $hostAddress: ${e.message}")
                    }
                }

            return deviceList
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: List<String>) {
            callback?.onScanCompleted(result)
        }
    }

    interface ScanCallback {
        fun onScanCompleted(devices: List<String>)
    }
}

@Composable
fun MainActivity.DisplayPlugNetworks(activity: MainActivity, plugWifiNetworks: List<String>, status: (Int) -> Unit, state:Int){
    Log.d("hi again", "It should be scanning now?")

    // For each network add a button to connect
    plugWifiNetworks .forEach { ssid ->
        Button(onClick = {
            if(wifiManager.isWifiEnabled){
                activity.connectToOpenWifi(ssid, status, state = state)
                Log.d("Initialise", "WiFi is turned on, connecting to plug")
            }else{
                Toast.makeText(
                    this,
                    "WiFi must be turned on",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.d("test", "connect_to_jamie_is_trying: $ssid")

        },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text(ssid, color = Color.White)
        }
    }
}

// Adds a button to allow refresh of networks if it doesn't appear
@Composable
fun RefreshWifiButton(activity: MainActivity, status: (Int) -> Unit) {
    Button(onClick = {
        activity.updateWifiScan()
    },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
        Text("Refresh Networks", color = Color.White)
    }
}

// Adds a button to allow return to main menu
@Composable
fun ReturnWifiButton(status: (Int) -> Unit) {
    Button(onClick = {
        status(1)
    },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
        Text("Return to home", color = Color.White)
    }
}

// Adds a button to allow refresh of networks if it doesn't appear
@Composable
fun RefreshMifiButton(activity: MainActivity, status: (Int) -> Unit) {
    Button(onClick = {
        activity.updateWifiScan()
    },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
        Text("Refresh Networks", color = Color.White)
    }
}

@Composable
fun MainActivity.DisplayMifiNetworks(activity: MainActivity, status: (Int) -> Unit){
    Log.d("hi again", "It should be scanning now?")

    // For each network add a button to connect
    mifiNetworks.forEach { ssid ->
        Button(onClick = {
            mifiNetworks.clear()
            mifiNetworks.add(ssid)
            status(5)
        },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text(ssid, color = Color.White)
        }
    }
}

private var lastScanTime = 0L
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private fun MainActivity.updateWifiScan() {
    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
    val currentTime = System.currentTimeMillis()
    // Define the BroadcastReceiver to handle scan results
    if (currentTime - lastScanTime > (30 * 1000)) {   // If 30 seconds since last test
        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Check if scan results are updated
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    Log.d("test", "Scan Success")
                    wifiList() // Function to process scan results

                } else {
                    Log.d("test", "Scan failed")
                    Toast.makeText(
                        context,
                        "Scan failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
                unregisterReceiver(this)
            }
        }

        // Register the receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        // Start the scan
        wifiManager.startScan()
        lastScanTime = currentTime
    } else {
        Toast.makeText(
            this,
            "Next scan in ${30 - (currentTime - lastScanTime)/1000} seconds",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private fun MainActivity.wifiList() {
    Toast.makeText(
        this,
        "Scan successful, updating results now",
        Toast.LENGTH_SHORT
    ).show()
    val wifiFullnfo = wifiManager.scanResults // Find local networks

    // Find just the SSIDs
    val wifiNetworksList = wifiFullnfo.map { it.SSID }
    var filteredWifiNetworksList = wifiNetworksList.filter {
        it.contains("plug", ignoreCase = true) or it.contains(
            "tasmota",
            ignoreCase = true
        )
    }
    plugWifiNetworks.clear()
    plugWifiNetworks.addAll(filteredWifiNetworksList)

    filteredWifiNetworksList = wifiNetworksList.filter { it.contains("4g", ignoreCase = true) }
    mifiNetworks.clear()
    mifiNetworks.addAll(filteredWifiNetworksList)
    Log.d("test", "wifiManager_jamie_is_trying: $plugWifiNetworks")
}


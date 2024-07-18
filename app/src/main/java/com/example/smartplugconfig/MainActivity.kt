package com.example.smartplugconfig
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL


class MainActivity : ComponentActivity() {

    lateinit var wifiManager: WifiManager
    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = this, plugWifiNetworks = plugWifiNetworks)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun wifiManagerInitialisation(){

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun initialisation() {

        registerPermissions()
        wifiManagerInitialisation()
    }

    // Establishes all necessary permissions for a wifi scan
    private fun registerPermissions() {

        if (ActivityCompat.checkSelfPermission(       // If location permission isn't granted
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val accessFinePermission =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {    // Request permission for location
                    if (it) {   // If permission granted

                        Log.d(  // Sends notification of permission granted
                            "myWifiManager",
                            "Location Permission Granted"
                        )

                    } else {
                        // Permission is denied
                        // Show an error message
                    }
                }
            accessFinePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
    }

    // Connect to normal wifi
    @RequiresApi(Build.VERSION_CODES.Q)
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

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
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

    // Connect to open wifi (no password) temporary
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToOpenWifi(ssid: String) {
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
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.d("WifiConnection", "Connection to $ssid failed")
            }
        })
    }

}



class MainViewModel : ViewModel() {

    private val _ipAddress = mutableStateOf<String?>(null)
    val ipAddress: State<String?> = _ipAddress
    var plugWifiNetworks = mutableStateListOf<String>()
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
            modifier = Modifier.fillMaxSize()

        ) {
            RefreshWifiButton(activity = activity, status = status, state = state)
            DisplayPlugNetworks(activity, plugWifiNetworks, status = status)
            ReturnWifiButton(status = status)
        }
        return "Trying to connect to wifi"
    }

    fun turnOnHotspot(): String {
        // Implement the actual logic here
        // For now, just return a placeholder string
        return "Turning on hotspot..."
    }

    fun ipScan(): String {
        return "Scanning for IP Address..."
    }

    fun sendWifiConfig(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = sendWifiConfigInternal()
            onResult(result)
        }
    }

    private suspend fun sendWifiConfigInternal(): String {
        //uses default ip for tasmota plug wifi ap
        val urlString = "http://192.168.4.1/cm?cmnd=Backlog%20SSID1%20Pixel%3B%20Password1%20123456789%20WifiConfig%205"
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

@SuppressLint("UnrememberedMutableState")
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

    when (status) {
        1 -> {  //Default gives you all expected options


            Column(
                modifier = modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(250.dp))
                Button(onClick = {
                    Log.d("hi - should be 1", status.toString())
                    activity.wifiList()
                    status = 2
                    Log.d("bye - should be 2", status.toString())
                }) {
                    Text("Connect to Plug")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    viewModel.sendWifiConfig { result ->
                        setCurrentTextOutput(result)
                    }
                }) {
                    Text("Send Wifi config")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    val result = viewModel.turnOnHotspot()
                    setCurrentTextOutput(result)
                }) {
                    Text("Switch on Hotspot")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    viewModel.scanDevices(context) { result ->
                        if (result != null) {
                            setCurrentTextOutput(result)
                        }
                    }
                }) {
                    Text("find IP address of plug")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    viewModel.sendMQTTConfig { result ->
                        setCurrentTextOutput(result)
                    }
                }) {
                    Text("Send MQTT config")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    viewModel.getPowerReading { result ->
                        setCurrentTextOutput(result)
                    }
                }) {
                    Text("Pull power data")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(textToDisplay)
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

        else -> {}
    }
}



// code from facto, class used in ip scan functionality


class DeviceScanner(private val context: Context) {

    fun scanDevices(callback: ScanCallback?) {
        ScanTask(callback).execute()
    }

    inner class ScanTask(private val callback: ScanCallback?) : AsyncTask<Void, Void, List<String>>() {
        override fun doInBackground(vararg params: Void?): List<String> {
            val deviceList = mutableListOf<String>()
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcpInfo = wifiManager.dhcpInfo

            Log.d("DeviceScanner", "Starting scan in range 192.168.y.z")

            // Scan the range 192.168.y.z where y and z vary from 0 to 255
            for (y in 0..255) {
                for (z in 1..254) { // Skipping 0 and 255 for z as they are typically not used for hosts
                    val hostAddress = "192.168.$y.$z"

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
            }
            return deviceList
        }

        override fun onPostExecute(result: List<String>) {
            callback?.onScanCompleted(result)
        }
    }

    interface ScanCallback {
        fun onScanCompleted(devices: List<String>)
    }
}

@Composable
fun DisplayPlugNetworks(activity: MainActivity, plugWifiNetworks: List<String>, status: (Int) -> Unit){
    Log.d("hi again", "It should be scanning now?")

    // For each network add a button to connect
    plugWifiNetworks .forEach { ssid ->
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.connectToOpenWifi(ssid)
            } else {
                Log.e("Deprecation", "Android version is older than Android 10")
            }
            Log.d("test", "connect_to_jamie_is_trying: $ssid")
            status(1)
        }) {
            Text(ssid)
        }
    }
}

// Adds a button to allow refresh of networks if it doesn't appear
@Composable
fun RefreshWifiButton(activity: MainActivity, status: (Int) -> Unit, state: Int) {
    Button(onClick = {
        activity.wifiList()
        status(3)   // Yes I know this is awful code but this sets status to just the else section which has nothing and then back to original to refresh
    }) {
        Text("Refresh Networks")
    }
}

// Adds a button to allow return to main menu
@Composable
fun ReturnWifiButton(status: (Int) -> Unit) {
    Button(onClick = {
        status(1)
    }) {
        Text("Return to home")
    }
}

@SuppressLint("MissingPermission")
private fun MainActivity.wifiList() {
    initialisation() // Just checks that permissions are established
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


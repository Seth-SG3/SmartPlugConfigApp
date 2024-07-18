package com.example.smartplugconfig

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp()
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

    fun connectToPlugWifi(): String {
        // Implement the actual logic here
        // For now, just return a placeholder string
        return "Trying to connect to plug WiFi..."
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
                        "Response: $response"
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
fun SmartPlugConfigApp(viewModel: MainViewModel = viewModel()) {
    var currentTextOutput by remember { mutableStateOf("output") }
    val context = LocalContext.current

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,
        setCurrentTextOutput = { currentTextOutput = it },
        context = context,
        viewModel = viewModel
    )
}

@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(250.dp))
        Button(onClick = {
            val result = viewModel.connectToPlugWifi()
            setCurrentTextOutput(result)
        }) {
            Text("Connect to plug")
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




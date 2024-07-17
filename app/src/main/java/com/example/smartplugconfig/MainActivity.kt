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

@Preview
@Composable
fun SmartPlugConfigApp() {
    var currentTextOutput by remember { mutableStateOf("output") }
    val context = LocalContext.current
    ButtonsWithTextOutput(textToDisplay = currentTextOutput, setCurrentTextOutput = { currentTextOutput = it } , context = context)
}

@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val deviceScanner = DeviceScanner(context)

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(250.dp))
        Button(onClick = {
            val result = connectToPlugWifi()
            setCurrentTextOutput(result)
        }) {
            Text("Connect to plug")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                val result = sendWifiConfig()
                withContext(Dispatchers.Main) {
                    setCurrentTextOutput(result)
                }
            }
        }) {
            Text("Send Wifi config")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            val result = turnOnHotspot()
            setCurrentTextOutput(result)
        }) {
            Text("Switch on Hotspot")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            deviceScanner.scanDevices(object : DeviceScanner.ScanCallback {
                override fun onScanCompleted(devices: List<String>) {
                    val result = if (devices.isEmpty()) {
                        "No devices found"
                    } else {
                        devices.joinToString("\n")
                    }
                    setCurrentTextOutput(result)
                }
            })
        }) {
            Text("find IP address of plug")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                val result = sendMQTTConfig()
                withContext(Dispatchers.Main) {
                    setCurrentTextOutput(result)
                }
            }
        }) {
            Text("Send MQTT config")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                val result = getPowerReading()
                withContext(Dispatchers.Main) {
                    setCurrentTextOutput(result)
                }
            }
        }) {
            Text("Pull power data")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(textToDisplay)
    }
}

fun connectToPlugWifi(): String {
    // Implement the actual logic here
    // For now, just return a placeholder string
    return "Trying to connect to plug WiFi..."
}

suspend fun sendWifiConfig(): String {
    //cm?cmnd=Backlog%20SSID1%20Pixel%3B%20Password1%20123456789
    //cm?cmnd=Power%20off
    val urlString = "http://192.168.4.1/cm?cmnd=Backlog%20SSID1%20Pixel%3B%20Password1%20123456789%20WifiConfig%205"
    return try {
        Log.d("sendWifiConfig", "Attempting to send request to $urlString")
        val url = URL(urlString)
        withContext(Dispatchers.IO){
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // or "POST" if you need to send some data
                // Set any required headers here
                Log.d("sendWifiConfig", "Request method set to $requestMethod")

                // For POST request, write output stream
                // outputStream.write("Your request body".toByteArray())
                // outputStream.flush()

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


fun turnOnHotspot(): String {
    // Implement the actual logic here
    // For now, just return a placeholder string
    return "Turning on hotspot..."
}

fun ipScan(): String{
    return "Scanning for IP Address..."
}

suspend fun sendMQTTConfig(): String {
    val urlString = "http://192.168.240.238/cm?cmnd=Backlog%20MqttHost%20testHost%3B%20MqttUser%20Test1%3B%20MqttPassword%20Test2%3B%20Topic%20smartPlugTest"
    return try {
        Log.d("sendMQTTConfig", "Attempting to send request to $urlString")
        val url = URL(urlString)
        withContext(Dispatchers.IO) {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // or "POST" if you need to send some data
                // Set any required headers here
                Log.d("sendMQTTConfig", "Request method set to $requestMethod")

                // For POST request, write output stream
                // outputStream.write("Your request body".toByteArray())
                // outputStream.flush()

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

suspend fun getPowerReading(): String {
    val urlString = "http://192.168.240.238/cm?cmnd=Status%208"
    return try {
        Log.d("getPowerReading", "Attempting to send request to $urlString")
        val url = URL(urlString)
        withContext(Dispatchers.IO) {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // or "POST" if you need to send some data
                // Set any required headers here
                Log.d("getPowerReading", "Request method set to $requestMethod")

                // For POST request, write output stream
                // outputStream.write("Your request body".toByteArray())
                // outputStream.flush()

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


// code from facto ,c lass used in ip scan functionality


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
                        socket.connect(InetSocketAddress(hostAddress, 80), 20) // Increased timeout to 500ms
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




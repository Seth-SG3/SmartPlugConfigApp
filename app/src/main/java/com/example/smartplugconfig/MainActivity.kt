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
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp()
            }
        }
    }
}

@Preview
@Composable
fun SmartPlugConfigApp() {
    var currentTextOutput by remember { mutableStateOf("output") }
    ButtonsWithTextOutput(TextToDisplay = currentTextOutput, setCurrentTextOutput = { currentTextOutput = it })
}

@Composable
fun ButtonsWithTextOutput(TextToDisplay: String, setCurrentTextOutput: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(300.dp))
        Button(onClick = {
            val result = connectToPlugWifi()
            setCurrentTextOutput(result)
        }) {
            Text("Connect to plug")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            val result = sendWifiConfig()
            setCurrentTextOutput(result)
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
            val result = sendMQTTConfig()
            setCurrentTextOutput(result)
        }) {
            Text("Send MQTT config")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = {
            val result = getPowerReading()
            setCurrentTextOutput(result)
        }) {
            Text("Pull data point")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(TextToDisplay)
    }
}

fun connectToPlugWifi(): String {
    // Implement the actual logic here
    // For now, just return a placeholder string
    return "Trying to connect to plug WiFi..."
}

fun sendWifiConfig(): String {
    //cm?cmnd=Backlog%20SSID1%20Pixel%3B%20Password1%20123456789
    val urlString = "http://192.168.4.1/cm?cmnd=Power%20off"
    return try {
        Log.d("sendWifiConfig", "Attempting to send request to $urlString")
        val url = URL(urlString)
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

fun sendMQTTConfig(): String {
    val urlString = "http://192.168.201.167/cm?cmnd=Power%20off"
    return try {
        Log.d("sendMQTTConfig", "Attempting to send request to $urlString")
        val url = URL(urlString)
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
    } catch (e: Exception) {
        Log.e("sendMQTTConfig", "Exception occurred", e)
        "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
    }
}

fun getPowerReading(): String {
    // Implement the actual logic here
    // For now, just return a placeholder string
    return "Getting power reading..."
}




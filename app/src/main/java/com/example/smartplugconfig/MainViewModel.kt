package com.example.smartplugconfig

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

val ipAddress = mutableStateOf<String?>(null)
class MainViewModel : ViewModel() {


    fun setIpAddress(ip: String) {
        ipAddress.value = ip
    }

    companion object {
        @Volatile private var instance: MainViewModel? = null

        fun getInstance(): MainViewModel =
            instance ?: synchronized(this) {
                instance ?: MainViewModel().also { instance = it }
            }
    }

    fun scanDevices(onScanCompleted: (String?) -> Unit) {
        print("Scan Device is called")
        val deviceScanner = DeviceScanner()
        deviceScanner.scanDevices(object : DeviceScanner.ScanCallback {
            override fun onScanCompleted(devices: List<String>) {
                val result = if (devices.isEmpty()) {
                    "No devices found"
                } else {
                    devices.joinToString("\\n")
                }
                ipAddress.value = result
                onScanCompleted(result)
                // Cancel the scan once a device is found
            }
        })
        Log.d("ViewModel"," This should have updated ip value")
    }


    @Composable
    fun ConnectToPlugWifi(activity: MainActivity, plugWifiNetworks: SnapshotStateList<String>, onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton(activity = activity)
            activity.DisplayPlugNetworks(activity, plugWifiNetworks, onResult)
            ReturnWifiButton(onResult)
        }
    }

    @Composable
    fun ChooseMifiNetwork(activity: MainActivity, mifiNetwork: (String) -> Unit, onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshMifiButton(activity = activity)
            activity.DisplayMifiNetworks(onResult,mifiNetwork = mifiNetwork)
            ReturnWifiButton(onResult)
        }
    }

    fun connectPlugToMifi(password: String, ssid: String, onResult: (String?) -> Unit) {

        this.sendWifiConfig(ssid, password){
                result -> if (result.contains("error", ignoreCase = true)){
            Log.e("Error", "Couldn't connect plug to MiFi")
            onResult("ConnectionFailed")
        }else{
            Log.d("Success", "Plug and MiFi are connected")
            onResult("ConnectionSuccess")
        }
        }

    }

    private fun sendWifiConfig(ssid: String = "Pixel", password: String = "intrasonics", onResult: (String) -> Unit) {
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

    fun getPowerReading(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = getPowerReadingInternal()
            onResult(result)
        }
    }

    private suspend fun getPowerReadingInternal(): String {


        val ip = ipAddress.value

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

            return "ConnectionFailure"
        }
    }

}

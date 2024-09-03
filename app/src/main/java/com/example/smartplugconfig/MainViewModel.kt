package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.content.Context
import android.net.MacAddress
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartplugconfig.data.DeviceScanner
import com.example.smartplugconfig.data.fetchPowerServiceResponse
import com.example.smartplugconfig.data.wifiConfigUrl
import com.example.smartplugconfig.hotspot.UnhiddenSoftApConfigurationBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.concurrent.Executor

class MainViewModel : ViewModel() {
    val ipAddress = mutableStateOf<String?>(null)
    private val _ipAddressMQTT = mutableStateOf<String?>(null)
    private var plugMacAddress = ""

    fun findPlugMacAddress(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = findPlugMacAddressInternal()
            onResult(result)
        }

    }

    private suspend fun findPlugMacAddressInternal():String{

        val urlString =
            "http://${ipAddress.value}/cm?cmnd=Status%205"
        return try {
            Log.d("MacAddress", "Attempting to send request to $urlString")
            val url = URL(urlString)
            withContext(Dispatchers.IO) {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    Log.d("MacAddress", "Request method set to $requestMethod")

                    val responseCode = responseCode
                    Log.d("MacAddress", "Response code: $responseCode")
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = inputStream.bufferedReader().use(BufferedReader::readText)
                        Log.d("MacAddress", "Response: $response")
                        val jsonObject = JSONObject(response)
                        val statusNetObject = jsonObject.getJSONObject("StatusNET")
                        plugMacAddress = statusNetObject.getString("Mac")
                        statusNetObject.getString("Mac")

                    } else {
                        "HTTP error code: $responseCode"
                    }
                }
            }
        } catch (e: Exception) {
            val errorMessage = "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
            Log.e("MacAddress", errorMessage, e)
            errorMessage
        }
    }

    fun setIpAddress(ip: String) {
        ipAddress.value = ip
        Log.d("setIpAddress $this", ip)
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

    fun sendWifiConfig(ssid: String = "Pixel", password: String = "intrasonics", onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = sendWifiConfigInternal(ssid, password)
            onResult(result)
        }
    }

    private suspend fun sendWifiConfigInternal(ssid: String, password: String): String {
        //uses default ip for tasmota plug wifi ap
        val wifiConfigUrlString = wifiConfigUrl(ssid, password)
        return try {
            Log.d("sendWifiConfig", "Attempting to send request to $wifiConfigUrlString")
            val url = URL(wifiConfigUrlString)
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

        if (isLocalOnlyHotspotEnabled()) {
            return "Hotspot is already active."
        }

        //denotes the mac addresses of devices that are permitted to
        //join the hotspot network.

        var allowedClientMacs = listOf(
            MacAddress.fromString("e0:dc:ff:eb:5d:f8") // personal phone for testing and accessing web UI
        ).toMutableList() // Convert to mutable list

        // Check if plugMacAddress is not an empty string
        if (plugMacAddress != "") {
            // Add the plugMacAddress to the list
            allowedClientMacs.add(MacAddress.fromString(plugMacAddress))
        }


        startLocalOnlyHotspotWithConfig(
            context = context,
            config = UnhiddenSoftApConfigurationBuilder()
                .setSsid("Pixel")
                .setAutoshutdownEnabled(false)
                .setPassphrase(
                    passphrase = "intrasonics",
                    securityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                )
                .setHiddenSsid(false)
                //the next line must be commented out/set to false for the blocked client lists to work
                //allowed client list requires it to be true
                .setClientControlByUserEnabled((plugMacAddress != ""))
                .setAllowedClientList(allowedClientMacs)
                .build(),
            executor = null,
            callback = object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(reservation)
                    Log.d("Hotspot", "Hotspot started")
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.d("Hotspot", "Hotspot stopped")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    Log.d("Hotspot", "Hotspot failed with reason $reason")
                }
            })

        return "starting a new hotspot connection..."
    }

    @SuppressLint("NewApi")
    fun startLocalOnlyHotspotWithConfig(
        context: Context,
        config: SoftApConfiguration,
        executor: Executor?,
        callback: WifiManager.LocalOnlyHotspotCallback
    ) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        WifiManager::class.java.getMethod(
            "startLocalOnlyHotspot", SoftApConfiguration::class.java, Executor::class.java,
            WifiManager.LocalOnlyHotspotCallback::class.java,
        ).invoke(wifiManager, config, executor, callback)
    }

    fun sendMQTTConfig(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = sendMQTTConfigInternal()
            onResult(result)
        }
    }

    private suspend fun sendMQTTConfigInternal(): String {
        val ip = ipAddress.value
        val host = _ipAddressMQTT.value
        val topic = "smartPlug"

        if (ip == null){
            Log.d("sendMQTTConfig", "plug ip not found")
        }
        if (host == null){
            Log.d("sendMQTTConfig", "device ip not found")
        }

        val mqttUrlString =
            "http://${ip}/cm?cmnd=Backlog%20MqttHost%20$host%3B%20MqttUser%20Test1%3B%20MqttPassword%20Test2%3B%20Topic%20$topic%3B%20SetOption140%201%3B%20MqttRetry%2010%3B%20MqttWifiTimeout%2020000%3B%20TelePeriod%2060"
        return try {
            Log.d("sendMQTTConfig", "Attempting to send request to $mqttUrlString")
            val url = URL(mqttUrlString)
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
    private fun setPowerReadingCallback(callback: PowerReadingCallback) {
        powerReadingCallback = callback
    }

    fun getPowerReading(callback: PowerReadingCallback, onResult: (String) -> Unit) {
        viewModelScope.launch {
            setPowerReadingCallback(callback)
            setBrokerPowerReadingCallback(callback)
            val result = getPowerReadingInternal()
            onResult(result)
        }
    }

    private suspend fun getPowerReadingInternal(): String {

        val response = fetchPowerServiceResponse()
        // Use the result here
        Log.d("Retrofit", "response = $response")

        // Parse the JSON response
        val jsonObject = JSONObject(response)
        val statusSNS = jsonObject.getJSONObject("StatusSNS")
        val energy = statusSNS.getJSONObject("ENERGY")
        val power = energy.getInt("Power")
        // Return the formatted string
        return "Power: $power Watts"


    }



    //is only actually checking if device has ip but wifi should never be on so i think is ok for now at least for soak testing
    fun isLocalOnlyHotspotEnabled(): Boolean {
        //val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        Log.d(
                            "isLocalOnlyHotspotEnabled",
                            "Device IP Address: ${address.hostAddress}"
                        )
                        if (_ipAddressMQTT.value == null) {
                            _ipAddressMQTT.value = address.hostAddress
                        }
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("isLocalOnlyHotspotEnabled", "Device does not have an IP address")
            return false
        }
        Log.d("isLocalOnlyHotspotEnabled", "Cannot get IP address")
        return false

    }


    companion object {
        @Volatile private var instance: MainViewModel? = null

        fun getInstance(): MainViewModel =
            instance ?: synchronized(this) {
                instance ?: MainViewModel().also { instance = it }
            }
    }
}
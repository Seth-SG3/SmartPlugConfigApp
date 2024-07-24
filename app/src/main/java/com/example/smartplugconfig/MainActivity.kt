package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.State
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.hotspot.UnhiddenSoftApConfigurationBuilder
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executor


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requiredPermissions = arrayOf(     // Any required permissions
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )
    lateinit var wifiManager: WifiManager
    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        initialisation()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = this)
            }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
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

    fun connectToPlugWifi(context: Context): String {
        // Create an Intent to open the Wi-Fi settings
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)

        // Check if there is an activity that can handle this intent
        if (intent.resolveActivity(context.packageManager) != null) {
            // Start the settings activity
            context.startActivity(intent)
            return "Opening Wi-Fi settings..."
        } else {
            return "Unable to open Wi-Fi settings."
        }
    }


//    fun turnOnHotspot(context: Context): String {
//        // Create an Intent to open the hotspot settings
//        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
//
//        // Check if there is an activity that can handle this intent
//        if (intent.resolveActivity(context.packageManager) != null) {
//            // Start the settings activity
//            context.startActivity(intent)
//            return "Opening hotspot settings..."
//        } else {
//            return "Unable to open hotspot settings."
//        }
//    }


    fun sendWifiConfig(
        ssid: String = "Pixel",
        password: String = "intrasonics",
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = sendWifiConfigInternal(ssid, password)
            onResult(result)
        }
    }

    private suspend fun sendWifiConfigInternal(ssid: String, password: String): String {
        //uses default ip for tasmota plug wifi ap
        val urlString =
            "http://192.168.4.1/cm?cmnd=Backlog%20SSID1%20${ssid}%3B%20Password1%20${password}%3B%20WifiConfig%205%3B%20restart%201"
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
                .setSsid("Pixel")
                .setAutoshutdownEnabled(false)
                .setPassphrase(
                    passphrase = "intrasonics",
                    securityType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                )
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
        val ip = _ipAddress.value
        val host =
            "192.168.245.252"  //test values for mqtt broker app on my phone, still not working
        val topic = "smartPlugTest"

        val urlString =
            "http://${ip}/cm?cmnd=Backlog%20MqttHost%20$host%3B%20MqttUser%20Test1%3B%20MqttPassword%20Test2%3B%20Topic%20$topic"
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getPowerReading(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = getPowerReadingInternal(context)
            onResult(result)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getPowerReadingInternal(context: Context): String {
        val ip = _ipAddress.value
        val urlString = "http://${ip}/cm?cmnd=Status%208"
        var attempts = 0

        while (attempts < 3) {
            if (isLocalOnlyHotspotEnabled(context)) {
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
                                val response =
                                    inputStream.bufferedReader().use(BufferedReader::readText)
                                Log.d("getPowerReading", "Response: $response")

                                // Parse the JSON response
                                val jsonObject = JSONObject(response)
                                val statusSNS = jsonObject.getJSONObject("StatusSNS")
                                val energy = statusSNS.getJSONObject("ENERGY")
                                val power = energy.getInt("Power")

                                // Return the formatted string
                                return@withContext "Power: $power Watts"
                            } else {
                                return@withContext "HTTP error code: $responseCode"
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errorMessage = "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
                    Log.e("getPowerReading", errorMessage, e)
                    return errorMessage
                }
            } else {
                turnOnHotspot(context)
                delay(3000)
                attempts++
            }
        }
        return "Unable to send request after 3 attempts"
    }

    //is only actually checking if device has ip but wifi should never be on so i think is ok for now at least for soak testing
    private fun isLocalOnlyHotspotEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

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
            val dhcpInfo = wifiManager.dhcpInfo

            // Get the device's IP address
            val deviceIpAddress = getDeviceIpAddress() ?: return emptyList()
            val thirdOctet = deviceIpAddress.split(".")[2]

            Log.d("DeviceScanner", "Starting scan in range 192.168.$thirdOctet.z")

            // Scan the range 192.168.x.1 to 192.168.x.254
            for (z in 1..254) { // Skipping 0 and 255 for z as they are typically not used for hosts
                val hostAddress = "192.168.$thirdOctet.$z"

                // Log each host address being scanned
                Log.d("DeviceScanner", "Scanning IP: $hostAddress")

                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(hostAddress, 80), 200) // Increased timeout to 200ms
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

        private fun getDeviceIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceScanner", "Failed to get device IP address: ${e.message}")
            }
            return null
        }
    }

    interface ScanCallback {
        fun onScanCompleted(devices: List<String>)
    }
}




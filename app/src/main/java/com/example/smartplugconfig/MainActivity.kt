package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.WifiManagerProvider.connectivityManager
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import getPlugMacAddress

class MainActivity : ComponentActivity() {


    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()
    private lateinit var appInitialisation: AppInitialisation

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
        appInitialisation = AppInitialisation(applicationContext)
        appInitialisation.initialise()
        handlePermissionsAndBatteryOptimization()
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

    private fun handlePermissionsAndBatteryOptimization() {
        val permissionsToRequest = appInitialisation.getPermissionsToRequest()
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        }

        if (appInitialisation.needsBatteryOptimizationRequest()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${applicationContext.packageName}")
            }
            startActivity(intent)
        }
    }

    // Connect to open wifi (no password) temporary
    fun connectToOpenWifi(ssid: String, onResult: (String?) -> Unit) {
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
                    connectionSuccessful(ssid = ssid, onResult = onResult)
                    getPlugMacAddress()
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiConnection", "Connection to $ssid failed")
                    connectionFailed(ssid = ssid, onResult = onResult)
                }

            })
    }

    // Connect to normal wifi

    @SuppressLint("ServiceCast")
    fun connectToWifi(
        ssid: String,
        password: String,
        onResult: (String?) -> Unit
    ) {

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
                    connectionSuccessful(ssid = ssid, onResult)

                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d("WifiConnection", "Connection to $ssid failed")
                    connectionFailed(ssid = ssid, onResult)
                }
            })
    }

    fun connectionSuccessful(ssid: String, onResult: (String?) -> Unit) {
        Toast.makeText(
            this,
            "Connected to Wifi $ssid",
            Toast.LENGTH_SHORT
        ).show()
        onResult("Success")

    }

    fun connectionFailed(ssid: String, onResult: (String?) -> Unit) {
        Toast.makeText(
            this,
            "Connection to WiFi failed, please retry $ssid",
            Toast.LENGTH_LONG
        ).show()
        onResult("Failure")
    }
    @Composable
    fun DataCycle() {
        // Start the PowerReadingService

        val intent = Intent(this, PowerReadingService::class.java)
        startService(intent)
    }

}

@Composable
fun SmartPlugConfigApp(viewModel: MainViewModel = viewModel(), activity: MainActivity, plugWifiNetworks: SnapshotStateList<String>) {
    val currentTextOutput by remember { mutableStateOf("output") }

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,

        viewModel = viewModel,
        activity = activity,
        plugWifiNetworks = plugWifiNetworks
    )
}
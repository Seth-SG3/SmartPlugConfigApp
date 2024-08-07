package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Adds a button to allow refresh of networks if it doesn't appear
@Composable
fun RefreshWifiButton(activity: MainActivity) {
    Button(onClick = {
        activity.updateWifiScan()
    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
        Text("Refresh Networks", color = Color.White)
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
fun RefreshMifiButton(activity: MainActivity) {
    Button(onClick = {
        activity.updateWifiScan()
    },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
        Text("Refresh Networks", color = Color.White)
    }
}

@Composable
fun MainActivity.DisplayMifiNetworks(status: (Int) -> Unit, mifiNetwork: (String) -> Unit){
    Log.d("hi again", "It should be scanning now?")

    // For each network add a button to connect
    mifiNetworks.forEach { ssid ->
        Button(onClick = {
            mifiNetwork(ssid)
            status(5)
        },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text(ssid, color = Color.White)
        }
    }
}

private var lastScanTime = 0L
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
fun MainActivity.updateWifiScan() {
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
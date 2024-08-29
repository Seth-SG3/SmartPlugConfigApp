package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import com.example.smartplugconfig.data.WifiManagerProvider.wifiManager

class CAWifiManager{
    private var lastScanTime = 0L
    val repository = NetworkRepository()

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun updateWifiScan() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime > (30 * 1000)) {   // If 30 seconds since last test

            // Start the scan
            wifiManager.startScan()
            lastScanTime = currentTime
            scanResultProcessing()
        } else {
            Toast.makeText(
                AppContext.getContext(),
                "Next scan in ${30 - (currentTime - lastScanTime)/1000} seconds",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun scanResultProcessing() {
        Log.d(repository.thisRetriever(), "<-----------------------------------")
        val wifiFullnfo = wifiManager.scanResults // Find local networks

        // Find just the SSIDs
        val wifiNetworksList = wifiFullnfo.map { it.SSID }
        var filteredWifiNetworksList = wifiNetworksList/*.filter {
            it.contains("plug", ignoreCase = true) or it.contains(
                "tasmota",
                ignoreCase = true
            )
        }
*/
        repository.clearPlugWifiNetworks()
        repository.addPlugWifiNetworkList(filteredWifiNetworksList)

        filteredWifiNetworksList = wifiNetworksList.filter { it.contains("4g", ignoreCase = true) }
        repository.clearMifiNetworks()
        repository.addMifiNetworkList(filteredWifiNetworksList)
    }
}
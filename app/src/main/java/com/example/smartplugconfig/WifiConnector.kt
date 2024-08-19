package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.util.Log
import android.widget.Toast
import com.example.smartplugconfig.WifiManagerProvider.connectivityManager
import getPlugMacAddress

object WifiConnector {


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
        val context = AppContext.getContext()

        Toast.makeText(
            context,
            "Connected to Wifi $ssid",
            Toast.LENGTH_SHORT
        ).show()
        onResult("Success")

    }

    fun connectionFailed(ssid: String, onResult: (String?) -> Unit) {
        val context = AppContext.getContext()

        Toast.makeText(
            context,
            "Connection to WiFi failed, please retry $ssid",
            Toast.LENGTH_LONG
        ).show()
        onResult("Failure")
    }

}
package com.example.smartplugconfig.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AppInitialisation(private val context: Context) {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
    )

    fun initialise() {
        WifiManagerProvider.initialise(context)
    }

    fun getPermissionsToRequest(): Array<String> {
        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    fun needsBatteryOptimizationRequest(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}



object WifiManagerProvider {
    lateinit var wifiManager: WifiManager
    lateinit var connectivityManager: ConnectivityManager

    fun initialise(context: Context) {
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    }
}

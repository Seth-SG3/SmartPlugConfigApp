package com.example.smartplugconfig

import android.content.Context
import android.net.ConnectivityManager

class ConnectivityManagerProvider(context: Context) {
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getConnectivityManager(): ConnectivityManager {
        return connectivityManager
    }
}
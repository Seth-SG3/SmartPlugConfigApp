package com.example.smartplugconfig

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

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
                    socket.connect(InetSocketAddress(hostAddress, 80), 30) // reduced from 200, seems to work fine
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
package com.example.smartplugconfig.data

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log
import com.example.smartplugconfig.data.WifiManagerProvider.wifiManager
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

// code from facto, class used in ip scan functionality
class DeviceScanner {

    private var scanTask: ScanTask? = null

    fun scanDevices(callback: ScanCallback?) {

        if (scanTask == null || scanTask?.status == AsyncTask.Status.FINISHED) {
            scanTask = ScanTask(callback)
            scanTask?.execute()
        } else {
            Log.d("DeviceScanner", "Scan already in progress")
        }
    }
    @SuppressLint("StaticFieldLeak")
    inner class ScanTask(private val callback: ScanCallback?) : AsyncTask<Void, Void, List<String>>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): List<String> {
            val deviceList = mutableListOf<String>()
            val dhcpInfo = wifiManager.dhcpInfo

            // Get the device's IP address
            val deviceIpAddress = getDeviceIpAddress() ?: return emptyList()
            val thirdOctet = deviceIpAddress.split(".")[2]


            // Scan the range 192.168.y.z where y and z vary from 0 to 255
            for (z in 2..254) { // Skipping 0 and 255 for z as they are typically not used for hosts
                val hostAddress = "192.168.$thirdOctet.$z"
                if (isCancelled) {
                    Log.d("DeviceScanner", "Scan cancelled before scanning $z")
                    return deviceList
                }
                // Log each host address being scanned
                Log.d("DeviceScanner", "Scanning IP: $hostAddress")

                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(hostAddress, 80), 100) // reduced from 200, seems to work fine
                    deviceList.add(hostAddress)
                    socket.close()

                    // Stop scanning once a device is found
                    if (deviceList.isNotEmpty()) {
                        Log.d("DeviceScanner", "Device found, stopping scan")
                        break
                    }

                } catch (e: IOException) {
                    Log.d("DeviceScanner", "Failed to connect to $hostAddress: ${e.message}")
                }
            }
            Log.d("scanner", "List returned")

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

    @Deprecated("Deprecated in Java")
    override fun onCancelled() {
        Log.d("DeviceScanner", "Scan task was cancelled.")
    }
    }

    interface ScanCallback {
        fun onScanCompleted(devices: List<String>)
    }
}
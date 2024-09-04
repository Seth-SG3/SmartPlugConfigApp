package com.example.smartplugconfig

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

// code from facto, class used in ip scan functionality, returns ip of th plug if connected.
class DeviceScanner {


    private var scanJob: Job? = null

    fun scanDevices(callback: ScanCallback?) {
        if (scanJob == null || scanJob?.isCompleted == true) {
            scanJob = CoroutineScope(Dispatchers.IO).launch {
                val deviceList = scanNetwork()
                withContext(Dispatchers.Main) {
                    callback?.onScanCompleted(deviceList)
                }
            }
        } else {
            Log.d("DeviceScanner", "Scan already in progress")
        }
    }

    private suspend fun scanNetwork(): List<String> {
        val deviceList = mutableListOf<String>()

        // Get the device's IP address
        val deviceIpAddress = getDeviceIpAddress() ?: return emptyList()
        val thirdOctet = deviceIpAddress.split(".")[2]

        // Scan the range 192.168.y.z where y and z vary from 0 to 255
        for (z in 2..254) { // Skipping 0 and 255 for z as they are typically not used for hosts
            val hostAddress = "192.168.$thirdOctet.$z"
            // Log each host address being scanned
            Log.d("DeviceScanner", "Scanning IP: $hostAddress")

            try {
                val socket = Socket()
                withTimeout(100L) {
                    socket.connect(InetSocketAddress(hostAddress, 80), 100)
                    deviceList.add(hostAddress)
                    socket.close()
                }

                // Stop scanning once a device is found
                if (deviceList.isNotEmpty()) {
                    Log.d("DeviceScanner", "Device found, stopping scan")
                    break
                }

            } catch (e: IOException) {
                Log.d("DeviceScanner", "Failed to connect to $hostAddress: ${e.message}")
            } catch (e: TimeoutCancellationException) {
                Log.d("DeviceScanner", "Connection timed out for IP: $hostAddress")
            }
        }
        Log.d("scanner", "List returned")
        return deviceList
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
    interface ScanCallback {
        fun onScanCompleted(devices: List<String>)
    }
}
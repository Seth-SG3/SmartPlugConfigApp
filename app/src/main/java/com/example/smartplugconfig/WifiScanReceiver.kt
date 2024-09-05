package com.example.smartplugconfig

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast

class WifiScanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if scan results are updated
        Log.d("ScanReceiver", "Scan result received")
        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        if (success) {
            Log.d("test", "Scan Success")
            CAWifiManager().scanResultProcessing() // Function to process scan results
        } else {
            Log.d("test", "Scan failed")
            Toast.makeText(context, "Scan failed", Toast.LENGTH_LONG).show()
        }
    }
}

package com.example.smartplugconfig

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.smartplugconfig.WifiManagerProvider.wifiManager
import java.lang.ref.WeakReference

object WifiButtons {

    @Composable
    fun ChoosePlugWifi(activity: WeakReference<MainActivity>, plugWifiNetworks: SnapshotStateList<String>, onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton(activity = activity)
            DisplayPlugNetworks(plugWifiNetworks, onResult)
            ReturnWifiButton(onResult)
        }
    }
    @Composable
    fun ChooseMifiNetwork(activity: WeakReference<MainActivity>, mifiNetwork: (String) -> Unit, onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton(activity = activity)
            activity.get()?.DisplayMifiNetworks(onResult,mifiNetwork = mifiNetwork)
            ReturnWifiButton(onResult)
        }
    }


    @Composable
    fun DisplayPlugNetworks(plugWifiNetworks: List<String>, onResult: (String?) -> Unit){
        Log.d("hi again", "It should be scanning now?")

        // For each network add a button to connect
        plugWifiNetworks .forEach { ssid ->
            Button(onClick = {
                if(wifiManager.isWifiEnabled){
                    WifiConnector.connectToOpenWifi(ssid, onResult = onResult)
                    Log.d("Initialise", "WiFi is turned on, connecting to plug")
                }else{
                    Toast.makeText(
                        AppContext.getContext(),
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
    fun ReturnWifiButton(onResult: (String?) -> Unit) {
        Button(onClick = {
            onResult("ReturnHome")
        },colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text("Return to home", color = Color.White)
        }
    }

    // Adds a button to allow refresh of networks if it doesn't appear
    @Composable
    fun RefreshWifiButton(activity: WeakReference<MainActivity>) {
        Button(onClick = {
            activity.get()?.updateWifiScan()
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text("Refresh Networks", color = Color.White)
        }
    }
}


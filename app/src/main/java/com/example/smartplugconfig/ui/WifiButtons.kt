package com.example.smartplugconfig.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartplugconfig.AppContext
import com.example.smartplugconfig.CAWifiManager
import com.example.smartplugconfig.NetworkRepository
import com.example.smartplugconfig.NetworkViewModel
import com.example.smartplugconfig.data.WifiConnector
import com.example.smartplugconfig.data.WifiManagerProvider.wifiManager

object WifiButtons {

    val CAWifiManager = CAWifiManager()

    @Composable
    fun ChoosePlugWifi(onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton()
            DisplayPlugNetworks(onResult = onResult)
            ReturnWifiButton(onResult)
        }
    }
    @Composable
    fun ChooseMifiNetwork(mifiNetwork: (String) -> Unit, onResult: (String?) -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF00B140))

        ) {
            RefreshWifiButton()
            DisplayMifiNetworks(onResult = onResult,mifiNetwork = mifiNetwork)
            ReturnWifiButton(onResult)
        }
    }


    @Composable
    fun DisplayPlugNetworks(
        networkViewModel: NetworkViewModel = viewModel(), // Add ViewModel parameter,
        onResult: (String?) -> Unit){
        Log.d("hi again", "It should be scanning now?")
        val plugWifiNetworks by networkViewModel.plugWifiNetworks.collectAsState()
        Log.d(networkViewModel.thisretrieval, "<-----------------------------------------")
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

    @Composable
    fun DisplayMifiNetworks(
        networkViewModel: NetworkViewModel = viewModel(),
        onResult: (String?) -> Unit,
        mifiNetwork: (String) -> Unit){
        Log.d("hi again", "It should be scanning now?")
        val mifiNetworks by networkViewModel.mifiNetworks.collectAsState()
        // For each network add a button to connect
        mifiNetworks.forEach { ssid ->
            Button(onClick = {
                mifiNetwork(ssid)
                onResult("ConnectToMifi")
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
    fun RefreshWifiButton() {
        Button(onClick = {
            CAWifiManager.updateWifiScan()
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0033A0))) {
            Text("Refresh Networks", color = Color.White)
        }
    }
}


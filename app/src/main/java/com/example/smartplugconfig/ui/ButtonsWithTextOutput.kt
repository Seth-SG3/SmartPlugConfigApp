package com.example.smartplugconfig.ui

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplugconfig.AppContext
import com.example.smartplugconfig.data.CHOOSE_MIFI_NETWORK
import com.example.smartplugconfig.data.CONNECT_CONSOLE_TO_MIFI
import com.example.smartplugconfig.data.CONNECT_TO_PLUG_WIFI
import com.example.smartplugconfig.data.DEFAULT_PAGE
import com.example.smartplugconfig.data.HOTSPOT_PAGE
import com.example.smartplugconfig.data.IP_SCANNING
import com.example.smartplugconfig.data.MIFI_TRANSITION_STATE
import com.example.smartplugconfig.MainActivity
import com.example.smartplugconfig.MainViewModel
import com.example.smartplugconfig.data.PLUG_WIFI_TRANSITION_STATE
import com.example.smartplugconfig.data.PROCESS_UNSUCCESSFUL_SCAN
import com.example.smartplugconfig.PowerReadingCallback
import com.example.smartplugconfig.data.SEND_PLUG_MIFI_INFO
import com.example.smartplugconfig.data.START_DATA_CYCLING
import com.example.smartplugconfig.data.WifiConnector

import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    activity: WeakReference<MainActivity>
) {
    var mifiSsid by remember { mutableStateOf("ssid") }
    var status by remember { mutableIntStateOf(1) }
    var scanCounter by remember { mutableIntStateOf(1) }
    val context = AppContext.getContext()

    val ipsosBlue = Color(0xFF0033A0) // Ipsos Blue color
    val ipsosGreen = Color(0xFF00B140) // Ipsos Green color
    var isScanning by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Scanning") }

    // LaunchedEffect to animate the loading text
    LaunchedEffect(isScanning) {
        while (isScanning) {
            loadingText = "Scanning"
            delay(500)
            loadingText = "Scanning."
            delay(500)
            loadingText = "Scanning.."
            delay(500)
            loadingText = "Scanning..."
            delay(500)
        }
    }
    when (status) {
        DEFAULT_PAGE -> {  //Default gives you all expected options

            /*

            TODO:
                If API level high enough display both options
                If Hotspot chosen then step through as Seth has implemented

             */


            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .background(ipsosGreen), // Set green background
                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Spacer(modifier = Modifier.height(250.dp))

                Button(
                    onClick = {
                        Log.d("hi - should be 1", status.toString())
                        status = CONNECT_TO_PLUG_WIFI
                        Log.d("bye - should be 2", status.toString())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
                ) {
                    Text("Connect to Plug", color = Color.White)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
                ) {
                    Text("1.3", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        viewModel.findPlugMacAddress{ result ->
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
                ) {
                    Text("get plug Mac Address via MQTT request", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isScanning) loadingText else textToDisplay,
                    fontSize = 20.sp, // Increase text size
                    fontWeight = FontWeight.Bold, // Make text bold
                    color = Color.Black // Text color
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        // IF API level high enough
                        // TODO setup Hotspot here
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            status = 50

                        } else {
                            Toast.makeText(
                                context, "Android version must be 13 or newer", Toast.LENGTH_SHORT
                            ).show()

                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
                ) {
                    Text("Use Hotspot", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isScanning) loadingText else textToDisplay,
                    fontSize = 20.sp, // Increase text size
                    fontWeight = FontWeight.Bold, // Make text bold
                    color = Color.Black // Text color
                )
            }
        }


        CONNECT_TO_PLUG_WIFI -> {
            Log.d("Status", "Status = $status")      // Allow connections to the plug wifi
            WifiButtons.ChoosePlugWifi { result ->
                if (result != null) {
                    when (result) {
                        "Success" -> {
                            status = CHOOSE_MIFI_NETWORK
                        }

                        "Failure" -> {
                            status = CONNECT_TO_PLUG_WIFI
                        }

                        "ReturnHome" -> {
                            status = 1
                        }
                    }
                }
            }
        }

        PLUG_WIFI_TRANSITION_STATE -> {
            Log.d("Status", "Status = $status")
            status = CONNECT_TO_PLUG_WIFI
        }

        CHOOSE_MIFI_NETWORK -> {
            Log.d("Status", "Status = $status")
            // Choose MiFi Network
            WifiButtons.ChooseMifiNetwork(
                mifiNetwork = { mifiSsid = it }) { result ->
                if (result != null) {
                    when (result) {
                        "ConnectToMifi" -> {
                            status = SEND_PLUG_MIFI_INFO
                        }

                        "ReturnHome" -> {
                            status = DEFAULT_PAGE
                        }
                    }
                }
            }

        }

        SEND_PLUG_MIFI_INFO -> {
            Log.d("Status", "Status = $status")
            // Send plug the mifi details

            Text(
                text = "Connecting plug to MiFi Device", fontSize = 20.sp, // Increase text size
                fontWeight = FontWeight.Bold, // Make text bold
                color = Color.Black // Text color
            )

            viewModel.connectPlugToMifi(
                ssid = mifiSsid, password = "1234567890"
            ) { result ->
                if (result != null) {
                    when (result) {
                        "ConnectionSuccess" -> {
                            status = CONNECT_CONSOLE_TO_MIFI
                        }

                        "ConnectionFailed" -> {
                            status = 1
                        }
                    }
                }
            }


        }

        CONNECT_CONSOLE_TO_MIFI -> {
            Log.d("Status", "Status = $status")
            // Connect to mifi device
            Text(
                text = "Connecting phone to MiFi device", fontSize = 20.sp, // Increase text size
                fontWeight = FontWeight.Bold, // Make text bold
                color = Color.Black // Text color
            )
            WifiConnector.connectToWifi(
                ssid = mifiSsid,
                password = "1234567890",
            ) { result ->
                if (result != null) {
                    when (result) {
                        "Success" -> {
                            status = IP_SCANNING
                        }

                        "Failure" -> {
                            status = MIFI_TRANSITION_STATE
                        }

                    }
                }
            }


        }

        MIFI_TRANSITION_STATE -> {
            Log.d("Status", "Status = $status")
            status = CHOOSE_MIFI_NETWORK
        }

        IP_SCANNING -> {
            Log.d("Status", "Status = $status")
            if (!isScanning) {
                isScanning = true
                viewModel.scanDevices { result ->
                    isScanning = false
                    if (result != null) {
                        if (result != "No devices found") {
                            result.let { ip -> viewModel.setIpAddress(ip) } // Set the IP address in the ViewModel.
                            scanCounter = 0
                            isScanning = false
                            status = START_DATA_CYCLING
                        } else {
                            status = PROCESS_UNSUCCESSFUL_SCAN
                        }
                    }
                }
            } else {
                Log.e("isScanning", "Scan already in progress")
            }
        }


        PROCESS_UNSUCCESSFUL_SCAN -> {
            Log.d("Status", "Status = $status")
            status = if (scanCounter < 3) {
                scanCounter + 1
                IP_SCANNING
            } else {

                CONNECT_TO_PLUG_WIFI
            }
        }

        START_DATA_CYCLING -> {
            Log.d("Status", "Status = $status")
            activity.get()?.DataCycle()
        }



        51 -> {
            Log.d("Status", "Status = $status")      // Allow connections to the plug wifi
            WifiButtons.ChoosePlugWifi(
            ) { result ->
                if (result != null) {
                    when (result) {
                        "Success" -> {
                            status = 50
                        }

                        "Failure" -> {
                            status = 51
                        }

                        "ReturnHome" -> {
                            status = 1
                        }
                    }
                }
            }
        }


        else -> {
            Log.d("Status", "Status = $status")
        }
    }
}


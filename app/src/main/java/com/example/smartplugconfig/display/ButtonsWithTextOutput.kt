package com.example.smartplugconfig.display

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplugconfig.MainViewModel
import kotlinx.coroutines.delay

//this is the composable that generates the ui for the hotspot version of the app, buttons run fucntions as labled and there is a text output on screen
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ButtonsWithTextOutput(
    textToDisplay: String,
    setCurrentTextOutput: (String) -> Unit,
    context: Context,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val ipsosBlue = Color(0xFF0033A0) // Ipsos Blue color
    val ipsosGreen = Color(0xFF00B140) // Ipsos Green color
    var isScanning by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Scanning") }
    var isHotspotEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isHotspotEnabled = viewModel.isLocalOnlyHotspotEnabled()
            delay(6000) // Check every minute
        }
    }

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

    val powerReading by viewModel.powerReadings.observeAsState()
    powerReading?.let {
        setCurrentTextOutput(it) // Update currentTextOutput when power reading is received
        Log.d("MQTT", "Updated textToDisplay: $it")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            //.padding(16.dp)
            .background(ipsosGreen), // Set green background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Icon(
            imageVector = if (isHotspotEnabled) Icons.Default.SignalWifi4Bar else Icons.Default.SignalWifiOff,
            contentDescription = null,
            tint = if (isHotspotEnabled) Color.Green else Color.Red,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = if (isHotspotEnabled) "Hotspot is ON" else "Hotspot is OFF",
            fontSize = 20.sp,
            color = if (isHotspotEnabled) Color.Green else Color.Red
        )
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = {
                val result = viewModel.connectToPlugWifi(context)
                viewModel.setCurrentTextOutput(result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue) // Set button color
        ) {
            Text("Connect to plug", color = Color.White) // Set text color to white for contrast
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.sendWifiConfig { result ->
                    viewModel.setCurrentTextOutput(result)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Send Wifi config", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                val result = viewModel.turnOnHotspot(context)
                viewModel.setCurrentTextOutput(result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Switch on Hotspot", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                isScanning = true
                viewModel.scanDevices { result ->
                    isScanning = false
                    viewModel.setCurrentTextOutput(result)
                    result.let { ip -> viewModel.setIpAddress(ip) } // Set the IP address in the ViewModel.
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Find IP address of plug", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.sendMQTTConfig { result ->
                    viewModel.setCurrentTextOutput(result)
                }

            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Send MQTT config", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.setupMQTTBroker(context)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("setup mqtt broker", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.findPlugMacAddress{ result ->
                    viewModel.setCurrentTextOutput(result)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("get plug Mac Address", color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                viewModel.getPowerReading()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ipsosBlue)
        ) {
            Text("Pull power data", color = Color.White)
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


package com.example.smartplugconfig

//noinspection UsingMaterialAndMaterial3Libraries

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.smartplugconfig.data.AppInitialisation
import com.example.smartplugconfig.ui.ButtonsWithTextOutput
import com.example.smartplugconfig.ui.theme.SmartPlugConfigTheme
import java.lang.ref.WeakReference


class MainActivity : ComponentActivity() {


    var mifiNetworks = mutableStateListOf<String>()
    var plugWifiNetworks = mutableStateListOf<String>()
    private lateinit var appInitialisation: AppInitialisation

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialisation()
        val weakActivity = WeakReference(this)

        enableEdgeToEdge()
        setContent {
            SmartPlugConfigTheme {
                SmartPlugConfigApp(activity = weakActivity, plugWifiNetworks = plugWifiNetworks)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initialisation() {
        appInitialisation = AppInitialisation(applicationContext)
        appInitialisation.initialise()
        handlePermissionsAndBatteryOptimization()
    }

    // Establishes all necessary permissions for a wifi scan
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                // Return back to code

            } else {
                Toast.makeText(
                    this,
                    "All permissions are required to start the hotspot",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun handlePermissionsAndBatteryOptimization() {
        val permissionsToRequest = appInitialisation.getPermissionsToRequest()
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        }

        if (appInitialisation.needsBatteryOptimizationRequest()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${applicationContext.packageName}")
            }
            startActivity(intent)
        }
    }

    @Composable
    fun DataCycle() {
        // Start the PowerReadingService

        val intent = Intent(this, PowerReadingService::class.java)
        startService(intent)
    }

}

@Composable
fun SmartPlugConfigApp(viewModel: MainViewModel = MainViewModel.getInstance(), activity: WeakReference<MainActivity>, plugWifiNetworks: SnapshotStateList<String>) {
    val currentTextOutput by remember { mutableStateOf("output") }

    ButtonsWithTextOutput(
        textToDisplay = currentTextOutput,

        viewModel = viewModel,
        activity = activity,
        plugWifiNetworks = plugWifiNetworks
    )
}















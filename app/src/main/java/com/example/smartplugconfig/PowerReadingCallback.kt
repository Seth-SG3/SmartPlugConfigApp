package com.example.smartplugconfig

interface PowerReadingCallback {
    fun onPowerReadingReceived(power: String)
}
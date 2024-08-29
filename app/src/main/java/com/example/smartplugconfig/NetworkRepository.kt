package com.example.smartplugconfig

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkRepository {

    private val _mifiNetworks = MutableStateFlow<List<String>>(emptyList())
    val mifiNetworks: StateFlow<List<String>> get() = _plugWifiNetworks
    private val _plugWifiNetworks = MutableStateFlow<List<String>>(emptyList())
    val plugWifiNetworks: StateFlow<List<String>> get() = _plugWifiNetworks
    fun thisRetriever():String{
        return this.toString()
    }

    fun addMifiNetwork(ssid: String) {
        _mifiNetworks.value += ssid    }

    fun addMifiNetworkList(ssidList:  List<String>) {
        _mifiNetworks.value += ssidList
    }


    fun addPlugWifiNetwork(ssid: String) {
        _plugWifiNetworks.value += ssid
    }

    fun addPlugWifiNetworkList(ssidList:  List<String>) {
        _plugWifiNetworks.value += ssidList
    }


    fun getMifiNetworks(): MutableStateFlow<List<String>> {
        return _mifiNetworks
    }

    fun getPlugWifiNetworks(): MutableStateFlow<List<String>> {
        return _plugWifiNetworks
    }

    fun clearPlugWifiNetworks(){
        _plugWifiNetworks.value = emptyList()
    }

    fun clearMifiNetworks(){
        _mifiNetworks.value = emptyList()
    }
}

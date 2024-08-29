package com.example.smartplugconfig

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class NetworkViewModel(repository: NetworkRepository = NetworkRepository()) : ViewModel() {
    val plugWifiNetworks: StateFlow<List<String>> = repository.plugWifiNetworks
    val mifiNetworks: StateFlow<List<String>> = repository.mifiNetworks
    val thisretrieval : String = repository.thisRetriever()
}
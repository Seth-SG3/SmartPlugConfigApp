package com.example.smartplugconfig.data

// Status values for pages

const val DEFAULT_PAGE = 1
const val CONNECT_TO_PLUG_WIFI = 2
const val PLUG_WIFI_TRANSITION_STATE = 3
const val CHOOSE_MIFI_NETWORK = 4
const val SEND_PLUG_MIFI_INFO = 5
const val CONNECT_CONSOLE_TO_MIFI = 6
const val MIFI_TRANSITION_STATE = 7
const val IP_SCANNING = 8
const val PROCESS_UNSUCCESSFUL_SCAN = 9
const val START_DATA_CYCLING = 10
const val HOTSPOT_PAGE = 50

const val AJAX_URL = "http://192.168.100.1/ajax"
const val PLUG_MAC_URL = "http://192.168.4.1/cm?cmnd=STATUS%205"

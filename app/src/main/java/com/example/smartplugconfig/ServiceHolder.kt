package com.example.smartplugconfig

object ServiceHolder {
    private var powerReadingService: PowerReadingService? = null

    fun setService(service: PowerReadingService) {
        powerReadingService = service
    }

    fun clearService() {
        powerReadingService = null
    }

    fun getService(): PowerReadingService? {
        return powerReadingService
    }
}
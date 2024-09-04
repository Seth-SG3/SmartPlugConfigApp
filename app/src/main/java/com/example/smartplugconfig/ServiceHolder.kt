package com.example.smartplugconfig

//used to pass the service so the lastTimeReceived cna be updated by the packet handler correctly.
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
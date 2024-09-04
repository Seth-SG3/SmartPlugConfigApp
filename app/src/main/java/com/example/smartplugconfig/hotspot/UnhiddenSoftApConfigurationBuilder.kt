package com.example.smartplugconfig.hotspot

import android.annotation.SuppressLint
import android.net.MacAddress
import android.net.wifi.SoftApConfiguration
import androidx.annotation.RequiresApi


//this was a gist found on a medium article with some additions made by me(seth) by reading the android source code
/**
 * Reflection workaround to access hidden SoftApConfiguration.Builder so it can be used to set
 * LocalOnlyHotspot on Android 13+
 *
 * LocalOnlyHotspotConfig is generated here by Android:
 * https://cs.android.com/android/platform/superproject/+/refs/heads/master:packages/modules/Wifi/service/java/com/android/server/wifi/WifiApConfigStore.java;drc=7bb4243a97d53af6cbd4de21bcc61556a758898b;l=423
 */
@RequiresApi(30)
class UnhiddenSoftApConfigurationBuilder {

    @SuppressLint("PrivateApi")
    private val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")

    private val builderInstance = builderClass.newInstance()

    fun setAutoshutdownEnabled(enabled: Boolean): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setAutoShutdownEnabled", Boolean::class.javaPrimitiveType).invoke(
            builderInstance, enabled
        )
        return this
    }

    fun setPassphrase(passphrase: String, securityType: Int): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod(
            "setPassphrase", String::class.java, Int::class.javaPrimitiveType
        ).invoke(
            builderInstance, passphrase, securityType
        )

        return this
    }

    fun setSsid(ssid: String): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setSsid", String::class.java).invoke(
            builderInstance, ssid
        )
        return this
    }


    fun setAllowedClientList(allowedClientList: List<MacAddress>): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setAllowedClientList", List::class.java).invoke(
            builderInstance, allowedClientList
        )
        return this
    }

    fun setBlockedClientList(blockedClientList: List<MacAddress>): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setBlockedClientList", List::class.java).invoke(
            builderInstance, blockedClientList
        )
        return this
    }

    //appraently this is required for whitelisting user connections.
    //https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Wifi/framework/java/android/net/wifi/SoftApConfiguration.java;drc=55141c5f4ae8f1141c97f35a66a27ba1239dd806;l=1955
    fun setClientControlByUserEnabled(enabled: Boolean): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setClientControlByUserEnabled", Boolean::class.javaPrimitiveType).invoke(
            builderInstance, enabled
        )
        return this
    }


    fun setHiddenSsid(hiddenSsid:Boolean): UnhiddenSoftApConfigurationBuilder {
        builderClass.getMethod("setHiddenSsid", Boolean::class.javaPrimitiveType).invoke(
            builderInstance, hiddenSsid
        )
        return this
    }






    fun build(): SoftApConfiguration {
        return builderClass
            .getMethod("build")
            .invoke(builderInstance) as SoftApConfiguration
    }
}

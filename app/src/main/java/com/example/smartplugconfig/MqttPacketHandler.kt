package com.example.smartplugconfig

import android.content.Context
import android.util.Log
import com.example.smartplugconfig.CsvUtils.saveToCsv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import mqtt.packets.mqtt.MQTTPublish
import org.json.JSONObject

//packet handler where new topics can be added to support future mqtt devices
class MqttPacketHandler {

    private val _powerReadingFlow = MutableSharedFlow<String>()
    val powerReadingFlow: SharedFlow<String> = _powerReadingFlow

    @OptIn(ExperimentalUnsignedTypes::class)
    fun packetRecieved(packet: MQTTPublish, context: Context) {
        val service = ServiceHolder.getService()
        if (service == null) {
            Log.e("MQTT", "PowerReadingService instance is not available")
            return
        }
        //more information about the topic names can be found at the following two links
        //https://tasmota.github.io/docs/MQTT/#fulltopic
        //https://tasmota.github.io/docs/Commands/

        //stat means the plug was polled for this message
        //status8 is the packet containing sensor data that can eb requested from the plug.
        if (packet.topicName == "stat/smartPlug/STATUS8") {
            //val powerReading = String(packet.payload,Charsets.UTF_8)
            val powerReadingRaw =
                packet.payload?.toByteArray()?.decodeToString()
            val jsonObject = powerReadingRaw?.let { JSONObject(it) }
            val power = jsonObject?.getJSONObject("StatusSNS")
                ?.getJSONObject("ENERGY")
                ?.getInt("Power")
            val powerReading = "Power: $power Watts"
            Log.d("MQTT", "got a power reading $powerReading")
            CoroutineScope(Dispatchers.Main).launch {
                _powerReadingFlow.emit(powerReading)
                Log.d("MQTT", "Power reading emitted: $powerReading")
            }
            Log.d("MQTT", "done")
        }

        //tele refers to the fact the plug broadcasts this data every minute(can be changed) and is not polled
        //Sensor refers to the packet type containing all relevant data of which we are scraping the power value.
        if (packet.topicName == "tele/smartPlug/SENSOR") {
            //val powerReading = String(packet.payload,Charsets.UTF_8).
            val powerReadingRaw =
                packet.payload?.toByteArray()?.decodeToString()
            val jsonObject = powerReadingRaw?.let { JSONObject(it) }
            val power =
                jsonObject?.getJSONObject("ENERGY")?.getInt("Power")
            val powerReading = "Power: $power Watts"
            Log.d("MQTT", "saving power to csv $powerReading")
            saveToCsv(context, powerReading)
            Log.d("MQTT", "resetting last received time")
            service.lastReceivedTime = System.currentTimeMillis()
        }
    }
}
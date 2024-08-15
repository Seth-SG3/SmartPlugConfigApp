package com.example.smartplugconfig

import MQTTClient
import android.content.Context
import android.util.Log
import com.example.smartplugconfig.CsvUtils.saveToCsv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mqtt.MQTTVersion
import mqtt.broker.Broker
import mqtt.broker.interfaces.PacketInterceptor
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTConnect
import mqtt.packets.mqtt.MQTTPublish
import org.json.JSONObject
import socket.tls.TLSClientSettings
import socket.tls.TLSSettings


@OptIn(ExperimentalUnsignedTypes::class)
fun sendMQTTmessage(command : String, payload : String? = "", host : String, port : Int) {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                val client = MQTTClient(
                    MQTTVersion.MQTT5,
                    host,
                    port,
                    TLSClientSettings(
                        serverCertificate = "/storage/emulated/0/Android/data/com.example.smartplugconfig/files/server.crt"
                    )
                ) {
                    println(it.payload?.toByteArray()?.decodeToString())
                }


               // Log.d("MQTT", "Publishing message...")
                client.publish(
                    false,
                    Qos.EXACTLY_ONCE,
                    "cmnd/smartPlug/$command",
                    "$payload".encodeToByteArray().toUByteArray()
                )
                //Log.d("MQTT", "Message published successfully.")

                Log.d("MQTT", "Running client...")
                client.run()
            }
            catch (e: Exception) {
                Log.e("MQTT", "Exception: ${e.message}", e)

            }



        }
    }
}

private var powerReadingCallback: PowerReadingCallback? = null

@ExperimentalUnsignedTypes
fun setupMqttBroker(context: Context){
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                Log.d("MQTT", "Running broker setup...")
                val broker = Broker(packetInterceptor = object : PacketInterceptor {
                    override fun packetReceived(clientId: String, username: String?, password: UByteArray?, packet: MQTTPacket) {
                        when (packet) {
                            is MQTTConnect -> Log.d("MQTT", "mqtt connect") //println(packet.protocolName)
                            is MQTTPublish -> { //Log.d("MQTT", "packet received ${packet.topicName}") //println(packet.topicName)
                                if (packet.topicName == "stat/smartPlug/STATUS8") {
                                    //val powerReading = String(packet.payload,Charsets.UTF_8)
                                    val powerReadingRaw = packet.payload?.toByteArray()?.decodeToString()
                                    val jsonObject = JSONObject(powerReadingRaw)
                                    val power = jsonObject.getJSONObject("StatusSNS")
                                        .getJSONObject("ENERGY")
                                        .getInt("Power")
                                    val powerReading = "Power: $power Watts"
                                    Log.d("MQTT", "got a power reading $powerReading")
                                    powerReadingCallback?.onPowerReadingReceived(powerReading)
                                    Log.d("MQTT", "done")
                                }
                                if (packet.topicName == "tele/smartPlug/SENSOR") {
                                    //val powerReading = String(packet.payload,Charsets.UTF_8)
                                    val powerReadingRaw = packet.payload?.toByteArray()?.decodeToString()
                                    val jsonObject = JSONObject(powerReadingRaw)
                                    val power = jsonObject.getJSONObject("ENERGY").getInt("Power")
                                    val powerReading = "Power: $power Watts"
                                    Log.d("MQTT", "saving power to csv $powerReading")
                                    saveToCsv(context,powerReading)
                                }
                                Log.d("MQTT", "packet received ${packet.topicName}")
                                Log.d("MQTT", "packet received ${packet.payload?.toByteArray()?.decodeToString()}")
                            }
                        }
                    }
                }, port = 8883,
                    tlsSettings = TLSSettings(keyStoreFilePath = "/storage/emulated/0/Android/data/com.example.smartplugconfig/files/keyStore.p12", keyStorePassword = "password")
                    )
                broker.listen()
                Log.d("MQTT", "broker setup :)")
            }
            catch (e: Exception) {
                Log.e("MQTT", "Exception: ${e.message}", e)

            }

        }
    }
}

fun setBrokerPowerReadingCallback(callback: PowerReadingCallback) {
    powerReadingCallback = callback
}
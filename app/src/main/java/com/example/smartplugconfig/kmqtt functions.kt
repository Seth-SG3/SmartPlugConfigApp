package com.example.smartplugconfig

import MQTTClient
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.broker.Broker
import mqtt.broker.interfaces.PacketInterceptor
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqtt.MQTTConnect
import mqtt.packets.mqtt.MQTTPublish
import mqtt.packets.mqttv5.SubscriptionOptions

@OptIn(ExperimentalUnsignedTypes::class)
fun sendMQTTmessage(command : String, payload : String? = "") {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                val client = MQTTClient(
                    MQTTVersion.MQTT5,
                    "192.168.222.246",
                    1883,
                    null
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
fun setupMqttBroker(){
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
                                    Log.d("MQTT", "got a power reading")
                                    val powerReading = packet.payload.toString()
                                    powerReadingCallback?.onPowerReadingReceived(powerReading)
                                }
                                Log.d("MQTT", "packet received ${packet.topicName}")
                                Log.d("MQTT", "packet received ${packet.payload}")
                            }
                        }
                    }
                })
                broker.listen()
                Log.d("MQTT", "broker setup :)")
            }
            catch (e: Exception) {
                Log.e("MQTT", "Exception: ${e.message}", e)

            }

        }
    }
}

fun setPowerReadingCallback(callback: PowerReadingCallback) {
    powerReadingCallback = callback
}
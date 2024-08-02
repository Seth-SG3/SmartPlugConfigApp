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
fun setupMQTTClient() {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                val client = MQTTClient(
                    MQTTVersion.MQTT3_1_1,
                    "192.168.222.246",
                    1883,
                    null
                ) {
                    println(it.payload?.toByteArray()?.decodeToString())
                }
                Log.d("MQTT", "Subscribing to topic...")
                client.subscribe(
                    listOf(
                        Subscription(
                            "cmnd/randomTopic",
                            SubscriptionOptions(Qos.EXACTLY_ONCE)
                        )
                    )
                )
                Log.d("MQTT", "Subscribed to topic successfully.")

                Log.d("MQTT", "Publishing message...")
                client.publish(
                    false,
                    Qos.EXACTLY_ONCE,
                    "cmnd/randomTopic/Power",
                    "TOGGLE".encodeToByteArray().toUByteArray()
                )
                Log.d("MQTT", "Message published successfully.")

                Log.d("MQTT", "Running client step...")
                client.run() // Blocking method, use step() if you don't want to block the thread.
                Log.d("MQTT", "Client step completed.")
            }
            catch (e: Exception) {
                Log.e("MQTT", "Exception: ${e.message}", e)

            }



        }
    }
}

fun setupMqttBroker(){
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                Log.d("MQTT", "Running broker step...")
                val broker = Broker(packetInterceptor = object : PacketInterceptor {
                    override fun packetReceived(clientId: String, username: String?, password: UByteArray?, packet: MQTTPacket) {
                        when (packet) {
                            is MQTTConnect -> Log.d("MQTT", "mqtt connect ${packet.protocolName}") //println(packet.protocolName)
                            is MQTTPublish -> Log.d("MQTT", "packet recieved ${packet.topicName}") //println(packet.topicName)
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
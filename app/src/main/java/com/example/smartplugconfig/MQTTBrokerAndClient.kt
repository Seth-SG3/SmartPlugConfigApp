package com.example.smartplugconfig

import MQTTClient
import android.content.Context
import android.util.Log
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

class MQTTBrokerAndClient {

    val packetHandler = MqttPacketHandler()

    @OptIn(ExperimentalUnsignedTypes::class)
    fun sendMQTTmessage(command: String, payload: String? = "", host: String, port: Int, topic: String) {
        //method to send a single mqtt command have removed all hard coded values
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                try {
                    val client = MQTTClient(
                        MQTTVersion.MQTT5,
                        host,
                        port = port,
                        null
                    ) {
                        println(it.payload?.toByteArray()?.decodeToString())
                    }


                    // Log.d("MQTT", "Publishing message...")
                    client.publish(
                        false,
                        Qos.EXACTLY_ONCE,
                        "cmnd/$topic/$command",
                        "$payload".encodeToByteArray().toUByteArray()
                    )
                    //Log.d("MQTT", "Message published successfully.")

                    Log.d("MQTT", "Running client...")
                    client.run()
                } catch (e: Exception) {
                    Log.e("MQTT", "Exception: ${e.message}", e)

                }


            }
        }
    }


    @ExperimentalUnsignedTypes
    fun setupMqttBroker(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                try {
                    Log.d("MQTT", "Running broker setup...")
                    val broker = Broker(
                        packetInterceptor = object : PacketInterceptor {
                            override fun packetReceived(
                                clientId: String,
                                username: String?,
                                password: UByteArray?,
                                packet: MQTTPacket
                            ) {
                                when (packet) {
                                    is MQTTConnect -> Log.d(
                                        "MQTT",
                                        "mqtt connect"
                                    ) //println(packet.protocolName)
                                    is MQTTPublish -> { //Log.d("MQTT", "packet received ${packet.topicName}") //println(packet.topicName)
                                        packetHandler.packetRecieved(packet, context)
                                    }
                                }
                            }
                        }, port = 8883
                        //the next line is how you enable encryption, requires keystore to be in listed directory so wont work on anything except the pixel 7a as is.
                        //tlsSettings = TLSSettings(keyStoreFilePath = "/storage/emulated/0/Android/data/com.example.smartplugconfig/files/keyStore.p12", keyStorePassword = "password")
                    )
                    broker.listen()
                    Log.d("MQTT", "broker setup :)")
                } catch (e: Exception) {
                    Log.e("MQTT", "Exception: ${e.message}", e)

                }

            }
        }
    }

}
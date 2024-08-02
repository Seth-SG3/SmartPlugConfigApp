package com.example.smartplugconfig

import MQTTClient
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(ExperimentalUnsignedTypes::class)
fun setupMQTTClient() {
    CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            val client = MQTTClient(
                MQTTVersion.MQTT3_1_1,
                "192.168.222.252",
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
                "Off".encodeToByteArray().toUByteArray()
            )
            Log.d("MQTT", "Message published successfully.")

            Log.d("MQTT", "Running client step...")
            client.step() // Blocking method, use step() if you don't want to block the thread.
            Log.d("MQTT", "Client step completed.")

        }
    }
}
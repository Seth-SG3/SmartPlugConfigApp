package com.example.smartplugconfig.data

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


fun restartMiFiDongle() {
    val client = OkHttpClient()

    // Prepare the JSON payload for restart request
    val jsonPayload = Gson().toJson(mapOf("funcNo" to 1013))
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonPayload.toRequestBody(mediaType)

    // Create the restart request
    val restartRequestBuilder = Request.Builder()
        .url(AJAX_URL)
        .post(requestBody)
        .header("Content-Type", "application/json")

    // Add cookies to the header if needed
    // cookies.forEach { restartRequestBuilder.addHeader("Cookie", it) }
    Log.d("Restart", "Attempting to restart mifi device")
    val restartRequest = restartRequestBuilder.build()

    client.newCall(restartRequest).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    println("Restart request failed with code: ${response.code}, message: ${response.message}")
                    throw IOException("Unexpected code $response")
                }
                println("MiFi dongle restarted successfully!")
            }
        }
    })
}

lateinit var phoneMacAddress: String
lateinit var plugMacAddress: String
fun getPlugMacAddress() {
    CoroutineScope(Dispatchers.Main).launch{
        try {
            val response = fetchPlugMac()
            Log.d("Get plug mac", "Plug Mac found as $response")
            plugMacAddress = response
        }catch(e:Exception){
            Log.e("MacAddress", "Failed to fetch Mac Address ${e.message}")

        }
    }
}

suspend fun fetchPlugMac(): String {
    return withContext(Dispatchers.IO){
            val response = fetchMacAddressResponse()

            //Parse the JSON response
            val jsonObject = JSONObject(response)
            val statusNET = jsonObject.getJSONObject("StatusNET")
            val result = statusNET.getString("Mac")
            result
    }
}

fun getPhoneMacAddress(){
    val client = OkHttpClient()
    // Prepare the JSON payload for restart request


    val jsonPayload = Gson().toJson(mapOf("funcNo" to 1011))
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonPayload.toRequestBody(mediaType)

    // Create the restart request
    val requestBuilder = Request.Builder()
        .url(AJAX_URL)
        .post(requestBody)
        .header("Content-Type", "application/json")

    Log.d("Restart", "Attempting to get mac addresses")
    val request = requestBuilder.build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    Log.d("Mac search failed with code: ${response.code}", "message: ${response.message}")
                    throw IOException("Unexpected code $response")

                }else {
                    val responseBody = response.body?.string()
                    Log.d("MacSearch", "Response: $responseBody")

                    // Parse the JSON response
                    val jsonObject = responseBody?.let { it1 -> JSONObject(it1) }
                    val resultsArray = jsonObject?.getJSONArray("results")

                    // Initialize a list to hold MAC addresses and device names
                    val deviceList = mutableListOf<Pair<String, String>>()

                    // Iterate through the results array
                    if (resultsArray != null) {
                        for (i in 0 until resultsArray.length()) {
                            val resultObject = resultsArray.getJSONObject(i)
                            val deviceArray = resultObject.getJSONArray("device_arr")

                            // Iterate through the device array
                            for (j in 0 until deviceArray.length()) {
                                val deviceObject = deviceArray.getJSONObject(j)
                                val macAddress = deviceObject.getString("mac")
                                val deviceName = deviceObject.getString("name")
                                if(deviceName.contains("Galaxy", ignoreCase = true)){
                                    phoneMacAddress = macAddress
                                }
                                // Add the MAC address and device name to the list
                                deviceList.add(Pair(macAddress, deviceName))
                            }
                        }
                        // Print the list of MAC addresses and device names
                        deviceList.forEach { (mac, name) ->
                            println("MAC Address: $mac, Device Name: $name")
                        }
                        whitelist()
                    }
                }
            }
        }
    })
}

fun whitelist(){
    Log.d("MAC", plugMacAddress)
    Log.d("MAC", phoneMacAddress)
    val macAddresses =  listOf(plugMacAddress, phoneMacAddress)
    var int = 1
    for (address in macAddresses) {

        val jsonPayload = Gson().toJson(
            mapOf(
                "funcNo" to 1054,
                "id" to int,
                "mac" to address
            )
        )
        int += 1
        sendMiFiRequest(jsonPayload = jsonPayload)

    }

    // Dongle uses a request of 1053 type 1 to say whitelist and then another of 1055 afterwards
    val jsonPayload1053 = Gson().toJson(
        mapOf(
            "funcNo" to 1053,
            "type" to "1",
        )
    )
    sendMiFiRequest(jsonPayload = jsonPayload1053)

    val jsonPayload1055 = Gson().toJson(
        mapOf(
            "funcNo" to 1055,
        )
    )
    sendMiFiRequest(jsonPayload = jsonPayload1055)


}

fun sendMiFiRequest(jsonPayload: String){

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonPayload.toRequestBody(mediaType)
    val client = OkHttpClient()


    // Create the request
    val sendMacRequestBuilder = Request.Builder()
        .url(AJAX_URL)
        .post(requestBody)
        .header("Content-Type", "application/json")

    Log.d("MAC Address", "Attempting to send request to MiFi")
    val sendMacRequest = sendMacRequestBuilder.build()

    client.newCall(sendMacRequest).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    println("Mifi request failed with: ${response.code}, message: ${response.message}")
                    throw IOException("Unexpected code $response")
                }
                println("Request sent successfully!")
            }
        }
    })

}
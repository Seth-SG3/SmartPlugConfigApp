import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


fun restartMiFiDongle() {
    val client = OkHttpClient()

    // Replace with actual URL and credentials
    val ajaxUrl = "http://192.168.100.1/ajax"


    // Prepare the JSON payload for restart request


    val jsonPayload = Gson().toJson(mapOf("funcNo" to 1013))
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = RequestBody.create(mediaType, jsonPayload)

    // Create the restart request
    val restartRequestBuilder = Request.Builder()
        .url(ajaxUrl)
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
fun getPlugMacAddress(){

    val urlString = "http://192.168.4.1/cm?cmnd=STATUS%205"
     try {
         Log.d("getPlugMAC", "Attempting to send request to $urlString")
         val url = URL(urlString)
         with(url.openConnection() as HttpURLConnection) {
             requestMethod = "GET"
             Log.d("getPlugMAC", "Request method set to $requestMethod")

             val responseCode = responseCode
             Log.d("getPlugMAC", "Response code: $responseCode")
             if (responseCode == HttpURLConnection.HTTP_OK) {
                 val response = inputStream.bufferedReader().use(BufferedReader::readText)
                 Log.d("getPlugMAC", "Response: $response")

                 // Parse the JSON response
                 val jsonObject = JSONObject(response)
                 val statusNET = jsonObject.getJSONObject("StatusNET")
                 plugMacAddress = statusNET.getString("Mac")

                 // Return the formatted string
                 Log.d("MacAddress", "Plug Mac Address : $plugMacAddress")

             } else {
                 "HTTP error code: $responseCode"
             }
         }

     } catch (e: Exception) {
         val errorMessage = "Error: ${e.localizedMessage ?: "An unknown error occurred"}"
         Log.e("getPlugMAC", errorMessage, e)
     }
}

fun getPhoneMacAddress(){


    val client = OkHttpClient()

    // Replace with actual URL and credentials
    val ajaxUrl = "http://192.168.100.1/ajax"


    // Prepare the JSON payload for restart request


    val jsonPayload = Gson().toJson(mapOf("funcNo" to 1011))
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = RequestBody.create(mediaType, jsonPayload)

    // Create the restart request
    val requestBuilder = Request.Builder()
        .url(ajaxUrl)
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
    val MACAddresses =  listOf(plugMacAddress, phoneMacAddress)
    val client = OkHttpClient()
    val ajaxUrl = "http://192.168.100.1/ajax"

    for (address in MACAddresses) {

        val jsonPayload = Gson().toJson(
            mapOf(
                "funcNo" to 1054,
                "id" to 1,
                "mac" to address
            )
        )
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = RequestBody.create(mediaType, jsonPayload)

        // Create the restart request
        val restartRequestBuilder = Request.Builder()
            .url(ajaxUrl)
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
}

/*

Send request to mifi to whitelist
Send other whitelist request

Format of request is
    {"funcNo":1054,"id":1,"mac":"34:98:7a:d5:f4:df"}:
*/
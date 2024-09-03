package com.example.smartplugconfig.data

import android.util.Log
import com.example.smartplugconfig.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.Response

fun createPlugRetrofitInstance(baseIpAddress: String): Retrofit {
    val baseUrl = "http://$baseIpAddress/"
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
}
interface PowerService {

    @GET("cm?cmnd=Status%208")
    suspend fun getResponse(): Response<String>
}

interface MacAddress {

    @GET("cm?cmnd=STATUS%205")
    suspend fun getResponse(): Response<String>
}

suspend fun <T> fetchRetrofitResponse(serviceClass: Class<T>): String {

    val viewModel: MainViewModel = MainViewModel.getInstance()
    val ip = viewModel.ipAddress.value
    if (ip!= null) {

        val retrofit = createPlugRetrofitInstance(ip)
        val retrofitService = retrofit.create(serviceClass)
        return try {
            val response = withContext(Dispatchers.IO) {
                // Use reflection to call the getResponse method
                val method = serviceClass.getDeclaredMethod("getResponse")
                method.invoke(retrofitService) as Response<String>
            }
            if (response.isSuccessful) {
                val listResult = response.body()
                Log.d("List", listResult ?: "No result")
                listResult ?: "No result" // Return the result here

            } else {
                Log.e("Retrofit", "Failed to fetch result: ${response.errorBody()?.string()}")
                "Error"
            }
        } catch (e: Exception) {
            Log.e("Retrofit", "Failed to fetch result: ${e.message}")
            "Error"
        }
    }else{
        Log.d("Retrofit", "ipaddress retrieval failed")
        return "Error"
    }
}

suspend fun fetchPowerServiceResponse(): String {
    return fetchRetrofitResponse(PowerService::class.java)
}

suspend fun fetchMacAddressResponse(): String {
    return fetchRetrofitResponse(MacAddress::class.java)
}
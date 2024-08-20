package com.example.smartplugconfig.data

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

fun createRetrofitInstance(baseIpAddress: String): Retrofit {
    val baseUrl = "http://$baseIpAddress/"
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
}
interface PowerService {

    @GET("cm?cmnd=Status%208")
    fun getPower(): String
}





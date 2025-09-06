package com.example.azarbijan_tech

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit API interface
interface ApiService {
    @GET("/")
    fun checkServer(): Call<String>

    @GET("/health")
    fun checkHealth(): Call<HealthResponse>

    @POST("/devices")
    fun registerDevice(@Body body: Map<String, String>): Call<DeviceResponse>

    @POST("/logs")
    fun sendScreenTime(@Body data: ScreenTimeData): Call<LogResponse>

    @GET("/logs")
    fun getScreenTimeLogs(): Call<List<ScreenTimeData>>

}

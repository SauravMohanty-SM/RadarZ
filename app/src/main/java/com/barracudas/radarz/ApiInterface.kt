package com.barracudas.radarz

import com.barracudas.radarz.Data.OpenWeatherMapData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("data/2.5/weather?appid=e2ed03ef864d2881b0ebdf70247b23ba")
    fun getData(@Query("lat") lat: String, @Query("lon") lon: String): Call<OpenWeatherMapData>
}
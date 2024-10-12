package com.example.sosapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassService {
    @GET("interpreter")
    fun getNearbyHospitals(
        @Query("data") query: String
    ): Call<HospitalResponse>
}

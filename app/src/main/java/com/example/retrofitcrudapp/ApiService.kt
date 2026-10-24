package com.example.retrofitcrudapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

        @GET("{raza}/images")
        fun getDogsByBreed(@Path("raza") raza: String?): Call<DogsResponse?>?
}
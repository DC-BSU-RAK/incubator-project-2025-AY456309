package com.example.vowera.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CurrencyRetrofitClient {
    private const val BASE_URL = "https://restcountries.com/v3.1/"

    val api: CountryCurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CountryCurrencyApiService::class.java)
    }
}
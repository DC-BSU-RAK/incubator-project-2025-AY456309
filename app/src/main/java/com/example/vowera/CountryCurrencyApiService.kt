package com.example.vowera.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET

data class CountryName(
    @SerializedName("common")
    val common: String? = null
)

data class CurrencyDetail(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("symbol")
    val symbol: String? = null
)

data class CountryCurrencyItem(
    @SerializedName("name")
    val name: CountryName? = null,

    @SerializedName("currencies")
    val currencies: Map<String, CurrencyDetail>? = null
)

interface CountryCurrencyApiService {

    @GET("all?fields=name,currencies")
    suspend fun getCountriesWithCurrencies(): Response<List<CountryCurrencyItem>>
}
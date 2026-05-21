package com.example.vowera.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class CountryItem(
    @SerializedName("country")
    val country: String? = null,

    @SerializedName("name")
    val name: String? = null
)

data class CountriesResponse(
    @SerializedName("error")
    val error: Boolean? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("data")
    val data: List<CountryItem>? = null
)

data class CitiesRequest(
    @SerializedName("country")
    val country: String
)

data class CitiesResponse(
    @SerializedName("error")
    val error: Boolean? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("data")
    val data: List<String>? = null
)

interface CountriesApiService {

    @GET("countries")
    suspend fun getCountries(): Response<CountriesResponse>

    @POST("countries/cities")
    suspend fun getCitiesByCountry(
        @Body request: CitiesRequest
    ): Response<CitiesResponse>
}
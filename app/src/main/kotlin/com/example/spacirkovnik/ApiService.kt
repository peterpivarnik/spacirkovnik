package com.example.spacirkovnik

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlin.jvm.java

interface ApiService {

    @GET("game.json")
    suspend fun getDataHolders(): GameResponse
}

private val retrofit = Retrofit.Builder().baseUrl("https://my-game-65b9c-default-rtdb.europe-west1.firebasedatabase.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

val dataHolderService: ApiService = retrofit.create(ApiService::class.java)

data class GameResponse(val holders: List<DataHolder>)
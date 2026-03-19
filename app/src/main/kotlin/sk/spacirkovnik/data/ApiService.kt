package sk.spacirkovnik.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import sk.spacirkovnik.model.GameDefinition
import sk.spacirkovnik.model.GameInfo

interface ApiService {

    @GET("games-index.json")
    suspend fun getGameIndex(): GameIndexResponse

    @GET("games/{gameId}.json")
    suspend fun getGame(@Path("gameId") gameId: String): GameDefinition
}

data class GameIndexResponse(
    val version: Int = 1,
    val games: List<GameInfo>
)

object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://my-game-65b9c-default-rtdb.europe-west1.firebasedatabase.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

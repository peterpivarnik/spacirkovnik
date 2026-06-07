package sk.spacirkovnik.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

const val MAPBOX_API_URL = "https://api.mapbox.com/"

/** Single LineString geometry returned when `geometries=geojson`. */
data class RouteGeometry(
    val coordinates: List<List<Double>> = emptyList(),
    val type: String = "",
)

data class DirectionsRoute(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: RouteGeometry? = null,
)

data class DirectionsResponse(
    val code: String = "",
    val routes: List<DirectionsRoute> = emptyList(),
)

interface DirectionsService {

    /**
     * Walking route between two points. [coords] is `lng,lat;lng,lat`
     * (kept un-encoded so the comma/semicolon reach Mapbox verbatim).
     */
    @GET("directions/v5/mapbox/walking/{coords}")
    suspend fun getWalkingRoute(
        @Path("coords", encoded = true) coords: String,
        @Query("access_token") accessToken: String,
        @Query("geometries") geometries: String = "geojson",
        @Query("overview") overview: String = "full",
    ): DirectionsResponse
}

object DirectionsRetrofit {
    private val retrofit = Retrofit.Builder()
        .baseUrl(MAPBOX_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: DirectionsService = retrofit.create(DirectionsService::class.java)
}

package sk.spacirkovnik.model

import com.google.gson.annotations.SerializedName

enum class GameStatus {
    @SerializedName("active") ACTIVE,
    @SerializedName("coming_soon") COMING_SOON,
    @SerializedName("purchasable") PURCHASABLE,
    @SerializedName("hidden") HIDDEN,
    UNKNOWN
}

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val version: Int = 1,
    val region: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val distanceKm: Double? = null,
    val status: GameStatus? = null,
    val startName: String? = null,
    val endName: String? = null
)

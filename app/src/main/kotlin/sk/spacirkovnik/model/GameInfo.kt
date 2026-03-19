package sk.spacirkovnik.model

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val version: Int = 1,
    val region: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val distanceKm: Double? = null
)

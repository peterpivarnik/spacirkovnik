package sk.spacirkovnik.model

data class GameScreen(
    val text: String? = null,
    val type: ScreenType? = null,
    val buttonText: String? = null,
    val backButtonText: String? = null,
    val nextButtonText: String? = null,
    val fontSize: Int? = null,
    val answers: List<GameAnswer>? = null,
    val targetLatitude: Double? = null,
    val targetLongitude: Double? = null,
    val imageUrl: String? = null
)

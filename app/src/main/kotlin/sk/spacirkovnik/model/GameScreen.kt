package sk.spacirkovnik.model

/**
 * Jedna obrazovka špacírky, načítaná z game-data/games/<id>.json.
 *
 * [directRoute] sa týka len obrazoviek typu NAVIGATION: predvolene sa kreslí skutočná trasa po
 * chodníkoch (Mapbox Directions), s `true` sa kreslí priamka od hráča k cieľu. Hodí sa tam, kde
 * chodníky v mapových podkladoch nie sú alebo vedú okľukou — napríklad na dostihovej dráhe.
 */
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
    val imageUrl: String? = null,
    val directRoute: Boolean = false
)

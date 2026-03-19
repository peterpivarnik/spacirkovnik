package sk.spacirkovnik.model

data class GameDefinition(
    val id: String,
    val title: String,
    val version: Int = 1,
    val screens: List<GameScreen>
)

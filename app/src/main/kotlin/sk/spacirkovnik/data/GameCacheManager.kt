package sk.spacirkovnik.data

import android.content.Context
import com.google.gson.Gson
import sk.spacirkovnik.model.GameDefinition
import java.io.File

class GameCacheManager(context: Context) {

    private val cacheDir = File(context.filesDir, "games").apply { mkdirs() }
    private val gson = Gson()

    fun saveGame(game: GameDefinition) {
        val file = File(cacheDir, "${game.id}.json")
        file.writeText(gson.toJson(game))
    }

    fun loadGame(gameId: String): GameDefinition? {
        val file = File(cacheDir, "$gameId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), GameDefinition::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedVersion(gameId: String): Int? {
        val game = loadGame(gameId) ?: return null
        return game.version
    }

}

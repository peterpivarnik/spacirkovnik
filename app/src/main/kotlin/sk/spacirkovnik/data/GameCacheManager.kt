package sk.spacirkovnik.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sk.spacirkovnik.model.GameDefinition
import sk.spacirkovnik.model.GameInfo
import java.io.File

class GameCacheManager(context: Context) {

    private val cacheDir = File(context.filesDir, "games").apply { mkdirs() }
    private val catalogFile = File(context.filesDir, "catalog.json")
    private val gson = Gson()

    fun saveCatalog(catalog: List<GameInfo>) {
        catalogFile.writeText(gson.toJson(catalog))
    }

    fun loadCatalog(): List<GameInfo>? {
        if (!catalogFile.exists()) return null
        return try {
            val type = object : TypeToken<List<GameInfo>>() {}.type
            gson.fromJson(catalogFile.readText(), type)
        } catch (_: Exception) {
            null
        }
    }

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

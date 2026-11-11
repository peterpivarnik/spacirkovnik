package sk.spacirkovnik.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sk.spacirkovnik.model.GameDefinition
import sk.spacirkovnik.model.GameInfo
import java.io.File

/**
 * Katalóg a stiahnuté hry uložené v telefóne. Všetky metódy, ktoré siahajú na súbory, bežia
 * mimo hlavného vlákna, aby obrazovka nečakala na disk.
 */
class GameCacheManager(context: Context) {

    private val cacheDir = File(context.filesDir, "games").apply { mkdirs() }
    private val catalogFile = File(context.filesDir, "catalog.json")
    private val versions = GameVersionIndex.shared(File(context.filesDir, "game-versions.json"), cacheDir)
    private val gson = Gson()

    suspend fun saveCatalog(catalog: List<GameInfo>) = withContext(Dispatchers.IO) {
        catalogFile.writeText(gson.toJson(catalog))
    }

    suspend fun loadCatalog(): List<GameInfo>? = withContext(Dispatchers.IO) {
        if (!catalogFile.exists()) return@withContext null
        try {
            val type = object : TypeToken<List<GameInfo>>() {}.type
            gson.fromJson<List<GameInfo>>(catalogFile.readText(), type)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveGame(game: GameDefinition) = withContext(Dispatchers.IO) {
        File(cacheDir, "${game.id}.json").writeText(gson.toJson(game))
        versions.put(game.id, game.version)
    }

    suspend fun loadGame(gameId: String): GameDefinition? = withContext(Dispatchers.IO) {
        val file = File(cacheDir, "$gameId.json")
        if (!file.exists()) return@withContext null
        try {
            gson.fromJson(file.readText(), GameDefinition::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /** Načíta zoznam verzií stiahnutých hier; volaj pred [getCachedVersion]. */
    suspend fun loadVersions() = withContext(Dispatchers.IO) {
        versions.load()
    }

    /** Verzia stiahnutej hry, alebo null, ak nie je stiahnutá. Číta len z pamäte. */
    fun getCachedVersion(gameId: String): Int? = versions.get(gameId)
}

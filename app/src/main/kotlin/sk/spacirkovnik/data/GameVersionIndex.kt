package sk.spacirkovnik.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sk.spacirkovnik.model.GameDefinition
import java.io.File

/**
 * Verzie stiahnutých hier v malom súbore (`{"rybar-z-drazdiaka": 3, …}`), aby zoznam hier
 * nemusel kvôli jednému číslu čítať a rozoberať celé súbory hier.
 *
 * Kto má hry stiahnuté zo staršej verzie appky, tento súbor ešte nemá — pri prvom [load] sa
 * preto vytvorí raz zo stiahnutých hier v [gamesDir].
 */
internal class GameVersionIndex(private val indexFile: File, private val gamesDir: File) {

    private val gson = Gson()
    private val lock = Any()
    private var versions: MutableMap<String, Int>? = null

    /** Načíta index zo súboru (alebo ho vytvorí zo stiahnutých hier). Siaha na disk. */
    fun load() {
        synchronized(lock) {
            if (versions == null) versions = readIndex() ?: rebuildFromGames().also { write(it) }
        }
    }

    /** Verzia stiahnutej hry z pamäte; pred [load] vždy null. */
    fun get(gameId: String): Int? = synchronized(lock) { versions?.get(gameId) }

    /** Zapíše verziu práve uloženej hry. Siaha na disk. */
    fun put(gameId: String, version: Int) {
        synchronized(lock) {
            load()
            val map = versions!!
            map[gameId] = version
            write(map)
        }
    }

    /** Zabudne stiahnutú hru (napr. poškodený súbor), aby sa stiahla znova. Siaha na disk. */
    fun remove(gameId: String) {
        synchronized(lock) {
            load()
            val map = versions!!
            if (map.remove(gameId) != null) write(map)
        }
    }

    private fun readIndex(): MutableMap<String, Int>? {
        if (!indexFile.exists()) return null
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(indexFile.readText(), type)?.toMutableMap()
        } catch (_: Exception) {
            null
        }
    }

    private fun rebuildFromGames(): MutableMap<String, Int> {
        val map = mutableMapOf<String, Int>()
        gamesDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            try {
                // Rovnako ako doteraz pri zisťovaní verzie: celý súbor hry, len tento jeden raz.
                val game = gson.fromJson(file.readText(), GameDefinition::class.java)
                if (game != null) map[file.nameWithoutExtension] = game.version
            } catch (_: Exception) {
                // Poškodený súbor berieme ako nestiahnutý — hra sa stiahne znova.
            }
        }
        return map
    }

    private fun write(map: Map<String, Int>) {
        try {
            indexFile.writeText(gson.toJson(map))
        } catch (_: Exception) {
            // Nevadí: index ostane v pamäti a nabudúce sa vytvorí znova zo stiahnutých hier.
        }
    }

    companion object {
        @Volatile private var shared: GameVersionIndex? = null

        /** Jeden index pre celú appku — hry ukladá zoznam, číta ich aj obrazovka hry. */
        fun shared(indexFile: File, gamesDir: File): GameVersionIndex =
            shared ?: synchronized(this) {
                shared ?: GameVersionIndex(indexFile, gamesDir).also { shared = it }
            }
    }
}

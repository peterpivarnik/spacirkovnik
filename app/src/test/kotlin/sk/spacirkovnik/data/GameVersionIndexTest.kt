package sk.spacirkovnik.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GameVersionIndexTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun setup(): Pair<File, File> {
        val gamesDir = tmp.newFolder("games")
        val indexFile = File(tmp.root, "game-versions.json")
        return indexFile to gamesDir
    }

    private fun writeGame(gamesDir: File, id: String, version: Int) {
        File(gamesDir, "$id.json").writeText(
            """{"id":"$id","title":"T","version":$version,"screens":[]}"""
        )
    }

    @Test
    fun beforeLoadNothingIsKnown() {
        val (indexFile, gamesDir) = setup()
        writeGame(gamesDir, "rybar", 3)
        assertNull(GameVersionIndex(indexFile, gamesDir).get("rybar"))
    }

    @Test
    fun firstLoadRebuildsFromDownloadedGamesAndWritesIndex() {
        val (indexFile, gamesDir) = setup()
        writeGame(gamesDir, "rybar", 3)
        writeGame(gamesDir, "vila", 1)
        File(gamesDir, "pokazena.json").writeText("{toto nie je json")

        val index = GameVersionIndex(indexFile, gamesDir).apply { load() }

        assertEquals(3, index.get("rybar"))
        assertEquals(1, index.get("vila"))
        assertNull(index.get("pokazena"))
        assertNull(index.get("nestiahnuta"))
        assertTrue(indexFile.exists())
    }

    @Test
    fun laterLoadsReadOnlyTheIndexFile() {
        val (indexFile, gamesDir) = setup()
        writeGame(gamesDir, "rybar", 3)
        GameVersionIndex(indexFile, gamesDir).load()

        // Súbor hry sa zmenil bez zápisu do indexu: index ho už nečíta, verí vlastnému súboru.
        writeGame(gamesDir, "rybar", 9)
        val reloaded = GameVersionIndex(indexFile, gamesDir).apply { load() }
        assertEquals(3, reloaded.get("rybar"))
    }

    @Test
    fun putUpdatesMemoryAndFile() {
        val (indexFile, gamesDir) = setup()
        val index = GameVersionIndex(indexFile, gamesDir).apply { load() }

        index.put("rybar", 4)
        assertEquals(4, index.get("rybar"))

        val reloaded = GameVersionIndex(indexFile, gamesDir).apply { load() }
        assertEquals(4, reloaded.get("rybar"))
    }

    @Test
    fun corruptIndexFileIsRebuilt() {
        val (indexFile, gamesDir) = setup()
        writeGame(gamesDir, "vila", 2)
        indexFile.writeText("nie je json")

        val index = GameVersionIndex(indexFile, gamesDir).apply { load() }
        assertEquals(2, index.get("vila"))
    }
}

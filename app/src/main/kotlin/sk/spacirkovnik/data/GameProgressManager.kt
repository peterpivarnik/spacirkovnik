package sk.spacirkovnik.data

import android.content.Context
import androidx.core.content.edit

class GameProgressManager(context: Context) {

    private val prefs = context.getSharedPreferences("game_progress", Context.MODE_PRIVATE)

    fun saveProgress(gameId: String, screenIndex: Int) {
        prefs.edit { putInt(gameId, screenIndex) }
    }

    fun getProgress(gameId: String): Int {
        return prefs.getInt(gameId, 0)
    }

    fun hasProgress(gameId: String): Boolean {
        return prefs.contains(gameId) && prefs.getInt(gameId, 0) > 0
    }

    fun clearProgress(gameId: String) {
        prefs.edit {
            remove(gameId)
            remove("started_$gameId")
        }
    }

    /** Zapamätá si, kedy hráč špacírku začal — ak ešte nie je zapísané. Pre čas na konci hry. */
    fun markStartedIfNeeded(gameId: String, nowMs: Long = System.currentTimeMillis()) {
        if (!prefs.contains("started_$gameId")) prefs.edit { putLong("started_$gameId", nowMs) }
    }

    fun getStartedAt(gameId: String): Long? =
        if (prefs.contains("started_$gameId")) prefs.getLong("started_$gameId", 0) else null

    fun markCompleted(gameId: String) {
        prefs.edit { putBoolean("completed_$gameId", true) }
    }

    fun isCompleted(gameId: String): Boolean {
        return prefs.getBoolean("completed_$gameId", false)
    }

    fun clearCompleted(gameId: String) {
        prefs.edit { remove("completed_$gameId") }
    }
}

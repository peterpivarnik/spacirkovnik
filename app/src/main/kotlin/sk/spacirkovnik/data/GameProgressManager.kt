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

    fun clearProgress(gameId: String) {
        prefs.edit { remove(gameId) }
    }
}

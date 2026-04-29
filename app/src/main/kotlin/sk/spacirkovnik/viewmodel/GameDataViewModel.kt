package sk.spacirkovnik.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import sk.spacirkovnik.data.GameCacheManager
import sk.spacirkovnik.data.GameCompletionManager
import sk.spacirkovnik.data.GameProgressManager
import sk.spacirkovnik.model.Gender
import sk.spacirkovnik.model.GameScreen

class GameDataViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheManager = GameCacheManager(application)
    private val progressManager = GameProgressManager(application)
    private val completionManager = GameCompletionManager()

    private val _index = mutableIntStateOf(0)
    private val _state = mutableStateOf(GameState())
    val state = _state

    private var currentGameId: String? = null

    fun loadGame(gameId: String) {
        if (currentGameId == gameId && _state.value.screens.isNotEmpty()) return

        currentGameId = gameId
        val game = cacheManager.loadGame(gameId)
        if (game != null && game.screens.isNotEmpty()) {
            val savedProgress = progressManager.getProgress(gameId)
            _index.intValue = savedProgress.coerceAtMost(game.screens.size - 1)
            _state.value = GameState(
                screens = game.screens,
                title = game.title,
                loading = false
            )
        } else {
            _state.value = GameState(
                loading = false,
                error = "Hra nebola nájdená alebo je prázdna. game=${game != null}, screens=${game?.screens?.size}"
            )
        }
    }

    fun incrementIndex() {
        if (_index.intValue < _state.value.screens.size - 1) {
            _index.intValue++
            saveProgress()
        }
    }

    fun decrementIndex() {
        if (_index.intValue > 0) {
            _index.intValue--
            saveProgress()
        }
    }

    fun getCurrentScreen(): GameScreen {
        return _state.value.screens[_index.intValue]
    }

    fun getScreenCount(): Int {
        return _state.value.screens.size
    }

    fun getCurrentIndex(): Int {
        return _index.intValue
    }

    fun isLastScreen(): Boolean {
        return _index.intValue >= _state.value.screens.size - 1
    }

    fun recordCompletion() {
        currentGameId?.let { completionManager.recordCompletion(it) }
    }

    fun clearProgress() {
        currentGameId?.let { progressManager.clearProgress(it) }
    }

    private fun saveProgress() {
        currentGameId?.let { progressManager.saveProgress(it, _index.intValue) }
    }

    private val _gender = mutableStateOf<Gender?>(null)
    val gender: State<Gender?> = _gender

    fun setGender(gender: Gender) {
        _gender.value = gender
    }

    data class GameState(
        val screens: List<GameScreen> = emptyList(),
        val title: String = "",
        val loading: Boolean = true,
        val error: String? = null
    )
}

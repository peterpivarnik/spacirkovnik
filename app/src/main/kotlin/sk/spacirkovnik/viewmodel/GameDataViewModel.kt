package sk.spacirkovnik.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sk.spacirkovnik.data.GameCacheManager
import sk.spacirkovnik.data.GameCompletionManager
import sk.spacirkovnik.data.GameProgressManager
import sk.spacirkovnik.data.ImagePrefetcher
import sk.spacirkovnik.data.reportNonFatal
import sk.spacirkovnik.model.FinishSummary
import sk.spacirkovnik.model.GameDefinition
import sk.spacirkovnik.model.Gender
import sk.spacirkovnik.model.GameScreen
import sk.spacirkovnik.model.finishSummary

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
        // Kým sa hra načíta z telefónu (na pozadí), ukáže sa načítavanie — nie obrazovky
        // predchádzajúcej hry.
        _state.value = GameState()
        viewModelScope.launch {
            val game = cacheManager.loadGame(gameId)
            // Hráč medzitým otvoril inú hru — tento výsledok už nepatrí na obrazovku.
            if (currentGameId != gameId) return@launch
            if (game == null || game.screens.isEmpty()) {
                // Súbor chýba alebo je poškodený: zabudneme ho, po návrate do zoznamu sa hra
                // stiahne znova (zoznam sa pri návrate obnoví).
                reportNonFatal(TAG, "Špacírku $gameId sa nepodarilo otvoriť", IllegalStateException(
                    "Cached game unusable: game=${game != null}, screens=${game?.screens?.size}"
                ))
                cacheManager.forgetGame(gameId)
            }
            showGame(gameId, game)
        }
    }

    private fun showGame(gameId: String, game: GameDefinition?) {
        if (game != null && game.screens.isNotEmpty()) {
            // Hry stiahnuté pred touto verziou appky obrázky vopred nemajú — doplníme ich teraz.
            ImagePrefetcher.prefetch(getApplication<Application>(), game)
            progressManager.markStartedIfNeeded(gameId)
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
                error = "Špacírku sa nepodarilo otvoriť. Vráť sa do zoznamu, stiahne sa znova."
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

    /**
     * Hráč dohral špacírku: zapíše dokončenie, vymaže postup a pripraví zhrnutie pre záverečnú
     * obrazovku (zastávky, úlohy, čas).
     */
    fun finishGame() {
        val gameId = currentGameId ?: return
        val startedAt = progressManager.getStartedAt(gameId)
        completionManager.recordCompletion(gameId)
        progressManager.markCompleted(gameId)
        progressManager.clearProgress(gameId)
        val current = _state.value
        _state.value = current.copy(
            finished = finishSummary(current.title, current.screens, startedAt, System.currentTimeMillis())
        )
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

    private companion object {
        const val TAG = "GameDataVM"
    }

    data class GameState(
        val screens: List<GameScreen> = emptyList(),
        val title: String = "",
        val loading: Boolean = true,
        val error: String? = null,
        val finished: FinishSummary? = null
    )
}

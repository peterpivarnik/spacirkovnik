package sk.spacirkovnik.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import sk.spacirkovnik.data.GameCacheManager
import sk.spacirkovnik.data.RetrofitInstance
import sk.spacirkovnik.model.GameInfo

class GameListViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheManager = GameCacheManager(application)
    private val _state = mutableStateOf(GameListState())
    val state: State<GameListState> = _state

    init {
        fetchGameIndex()
    }

    private fun fetchGameIndex() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.apiService.getGameIndex()
                cacheManager.saveCatalog(response)
                _state.value = GameListState(
                    games = buildGameList(response),
                    loading = false
                )
            } catch (e: Exception) {
                val cached = cacheManager.loadCatalog()
                if (cached != null) {
                    _state.value = GameListState(
                        games = buildGameList(cached),
                        loading = false,
                        offline = true
                    )
                } else {
                    val isNetworkError = e is java.net.UnknownHostException
                        || e is java.net.ConnectException
                        || e is java.net.SocketTimeoutException
                    _state.value = GameListState(
                        loading = false,
                        error = if (isNetworkError)
                            "Nie je dostupné internetové pripojenie."
                        else
                            "Nepodarilo sa načítať zoznam hier."
                    )
                }
            }
        }
    }

    private fun buildGameList(response: sk.spacirkovnik.data.GameIndexResponse) =
        response.games.filter { it.visible != false }.map { info ->
            val cachedVersion = cacheManager.getCachedVersion(info.id)
            val status = when {
                cachedVersion == null -> DownloadStatus.NOT_DOWNLOADED
                cachedVersion < info.version -> DownloadStatus.UPDATE_AVAILABLE
                else -> DownloadStatus.DOWNLOADED
            }
            GameWithStatus(info, status)
        }

    fun downloadGame(gameId: String) {
        viewModelScope.launch {
            updateGameStatus(gameId, DownloadStatus.DOWNLOADING)
            try {
                val game = RetrofitInstance.apiService.getGame(gameId)
                cacheManager.saveGame(game)
                updateGameStatus(gameId, DownloadStatus.DOWNLOADED)
            } catch (e: Exception) {
                updateGameStatus(gameId, DownloadStatus.NOT_DOWNLOADED)
                _state.value = _state.value.copy(
                    error = "Nepodarilo sa stiahnuť hru: ${e.message}"
                )
            }
        }
    }

    private fun updateGameStatus(gameId: String, status: DownloadStatus) {
        _state.value = _state.value.copy(
            games = _state.value.games.map {
                if (it.info.id == gameId) it.copy(status = status) else it
            }
        )
    }

    data class GameListState(
        val games: List<GameWithStatus> = emptyList(),
        val loading: Boolean = true,
        val offline: Boolean = false,
        val error: String? = null
    )

    data class GameWithStatus(
        val info: GameInfo,
        val status: DownloadStatus
    )

    enum class DownloadStatus {
        NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, UPDATE_AVAILABLE
    }
}

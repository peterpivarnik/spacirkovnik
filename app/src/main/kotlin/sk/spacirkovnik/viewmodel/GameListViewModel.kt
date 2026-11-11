package sk.spacirkovnik.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import sk.spacirkovnik.data.GameCacheManager
import sk.spacirkovnik.data.RetrofitInstance
import sk.spacirkovnik.data.reportNonFatal
import sk.spacirkovnik.model.GameInfo
import sk.spacirkovnik.model.GameStatus
import java.io.IOException

class GameListViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheManager = GameCacheManager(application)
    private val _state = mutableStateOf(GameListState())
    val state: State<GameListState> = _state

    private var rawCatalog: List<GameInfo> = emptyList()
    private var testGames: Set<String> = emptySet()

    init {
        fetchGameIndex()
    }

    fun refresh() {
        fetchGameIndex()
    }

    fun setTestGames(games: Set<String>) {
        testGames = games
        if (rawCatalog.isNotEmpty()) {
            _state.value = _state.value.copy(games = buildGameList(rawCatalog))
        }
    }

    private fun fetchGameIndex() {
        viewModelScope.launch {
            // Verzie stiahnutých hier treba mať v pamäti skôr, než sa zostaví zoznam.
            cacheManager.loadVersions()
            try {
                val response = RetrofitInstance.apiService.getGameIndex()
                cacheManager.saveCatalog(response)
                rawCatalog = response
                val gameList = buildGameList(response)
                _state.value = GameListState(games = gameList, loading = false)
                gameList
                    .filter { it.status == DownloadStatus.UPDATE_AVAILABLE }
                    .forEach { downloadGame(it.info.id) }
            } catch (e: Exception) {
                val cached = cacheManager.loadCatalog()
                if (cached != null) {
                    rawCatalog = cached
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
                            "Nepodarilo sa načítať zoznam špacírok."
                    )
                }
            }
        }
    }

    private fun buildGameList(response: List<GameInfo>) =
        response
            .filter { it.status != GameStatus.HIDDEN || testGames.contains(it.id) }
            .map { info ->
                val cachedVersion = cacheManager.getCachedVersion(info.id)
                val downloadStatus = when {
                    cachedVersion == null -> DownloadStatus.NOT_DOWNLOADED
                    cachedVersion < info.version -> DownloadStatus.UPDATE_AVAILABLE
                    else -> DownloadStatus.DOWNLOADED
                }
                GameWithStatus(info, downloadStatus)
            }

    fun downloadGame(gameId: String) {
        viewModelScope.launch {
            val previousStatus = _state.value.games.find { it.info.id == gameId }?.status
            updateGameStatus(gameId, DownloadStatus.DOWNLOADING)
            try {
                val game = RetrofitInstance.apiService.getGame(gameId)
                cacheManager.saveGame(game)
                updateGameStatus(gameId, DownloadStatus.DOWNLOADED)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Bez siete je to bežná vec; inak (odpoveď servera, poškodené dáta) chceme vedieť.
                if (e !is IOException) reportNonFatal(TAG, "Stiahnutie špacírky $gameId zlyhalo", e)
                if (previousStatus == DownloadStatus.UPDATE_AVAILABLE) {
                    // Nepodarila sa len aktualizácia — stará verzia ostáva v telefóne a dá sa hrať.
                    updateGameStatus(gameId, DownloadStatus.UPDATE_AVAILABLE)
                } else {
                    // Karta ukáže „Skúsiť znova"; zoznam ostáva, chyba ide len do krátkej hlášky.
                    updateGameStatus(gameId, DownloadStatus.NOT_DOWNLOADED)
                    _state.value = _state.value.copy(
                        downloadError = "Špacírku sa nepodarilo stiahnuť. Skontroluj pripojenie a skús to znova."
                    )
                }
            }
        }
    }

    fun clearDownloadError() {
        _state.value = _state.value.copy(downloadError = null)
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
        /** Zoznam sa nenačítal vôbec — obrazovka ukáže len túto hlášku. */
        val error: String? = null,
        /** Nepodarilo sa stiahnuť jednu špacírku — krátka hláška nad zoznamom, zoznam ostáva. */
        val downloadError: String? = null
    )

    data class GameWithStatus(
        val info: GameInfo,
        val status: DownloadStatus
    )

    private companion object {
        const val TAG = "GameListVM"
    }

    enum class DownloadStatus {
        NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, UPDATE_AVAILABLE
    }
}

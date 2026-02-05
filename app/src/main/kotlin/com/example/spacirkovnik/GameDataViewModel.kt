package com.example.spacirkovnik

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.collections.elementAt

class GameDataViewModel() : ViewModel() {

    private val _index: MutableIntState = mutableIntStateOf(0)
    private val _holderState = mutableStateOf(HolderState())
    val index: MutableState<Int> = _index
    val holdersState: State<HolderState> = _holderState

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            try {
                val gameResponse = dataHolderService.getDataHolders()
                _holderState.value = HolderState(list = gameResponse.holders,
                                                 loading = false,
                                                 error = null)
            }
            catch (e: Exception) {
                _holderState.value = HolderState(loading = false,
                                                 error = "Error fetching data ${e.message}")
            }
        }
    }

    fun incrementIndex() {
        _index.intValue++
    }

    fun decrementIndex() {
        _index.intValue--
    }

    fun getCurrentDataHolder(): DataHolder {
        return _holderState.value.list.elementAt(_index.intValue)
    }

    fun getHoldersListSize(): Int {
        return _holderState.value.list.size
    }

    fun getCurrentIndex(): Int {
        return index.value
    }

    data class HolderState(val loading: Boolean = true,
                           val list: List<DataHolder> = emptyList(),
                           val error: String? = null)

}
package sk.spacirkovnik.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import sk.spacirkovnik.model.LocationData

class LocationViewModel : ViewModel() {
    private val _location = mutableStateOf<LocationData?>(null)
    val location: State<LocationData?> = _location

    private val _isDebugMode = mutableStateOf(false)
    val isDebugMode: State<Boolean> = _isDebugMode

    fun updateLocation(newLocation: LocationData) {
        if (_isDebugMode.value) return
        _location.value = newLocation
    }

    fun setDebugLocation(newLocation: LocationData) {
        _isDebugMode.value = true
        _location.value = newLocation
    }

    fun clearDebugLocation() {
        _isDebugMode.value = false
    }
}

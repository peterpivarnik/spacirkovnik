package sk.spacirkovnik

import android.annotation.SuppressLint
import android.app.Application
import com.mapbox.common.MapboxOptions
import com.mapbox.common.TelemetryUtils

class SpacirkovnikApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setMapboxAccessToken()
        disableMapboxTelemetry()
    }

    // Set the token programmatically (Mapbox v11 way). This also references the string resource
    // in code, so R8 resource shrinking can't strip it from release builds — which previously
    // left the token empty and crashed the map with MapboxConfigurationException.
    private fun setMapboxAccessToken() {
        val token = getString(R.string.mapbox_access_token)
        if (token.isNotBlank()) {
            MapboxOptions.accessToken = token
        }
    }

    @SuppressLint("RestrictedApi")
    // TelemetryUtils.setEventsCollectionState je označené @RestrictTo(LIBRARY_GROUP_PREFIX),
    // no ide o jediné dostupné API na vypnutie zberu dát v Mapbox SDK v11.
    // Verejné alternatívy (MapboxOptions, SettingsService) túto možnosť neponúkajú.
    private fun disableMapboxTelemetry() {
        try {
            TelemetryUtils.setEventsCollectionState(false) { /* ignorujeme výsledok */ }
        } catch (_: Exception) {
            // Restricted API — ignorujeme ak nie je dostupné
        }
    }
}

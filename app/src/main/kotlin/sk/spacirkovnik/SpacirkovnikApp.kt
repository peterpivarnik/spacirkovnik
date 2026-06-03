package sk.spacirkovnik

import android.annotation.SuppressLint
import android.app.Application
import com.mapbox.common.TelemetryUtils

class SpacirkovnikApp : Application() {
    override fun onCreate() {
        super.onCreate()
        disableMapboxTelemetry()
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

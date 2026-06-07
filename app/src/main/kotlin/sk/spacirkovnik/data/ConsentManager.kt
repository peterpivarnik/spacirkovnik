package sk.spacirkovnik.data

import android.content.Context
import androidx.core.content.edit

/**
 * Local cache of accepted per-game consent versions, so a player who already agreed isn't
 * re-prompted when offline or before the Firebase state has loaded. The authoritative record
 * lives in Firebase (consents/{uid}/{gameId}); this just mirrors it on-device.
 */
class ConsentManager(context: Context) {

    private val prefs = context.getSharedPreferences("game_consent", Context.MODE_PRIVATE)

    /** Highest consent version accepted on this device, or -1 if none. */
    fun acceptedVersion(gameId: String): Int = prefs.getInt(gameId, -1)

    fun setAccepted(gameId: String, version: Int) {
        prefs.edit { putInt(gameId, version) }
    }
}

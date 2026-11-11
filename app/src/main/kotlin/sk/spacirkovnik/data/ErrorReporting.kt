package sk.spacirkovnik.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Technický detail chyby, ktorú používateľ vidí len ako ľudskú hlášku: zapíše ho do logu
 * a pošle do Crashlytics ako „non-fatal", aby bolo v konzole Firebase vidno, čo sa naozaj stalo.
 * Bežné výpadky siete sem neposielaj — tie nie sú chyba appky.
 */
fun reportNonFatal(tag: String, message: String, e: Throwable) {
    Log.w(tag, message, e)
    try {
        FirebaseCrashlytics.getInstance().apply {
            log("$tag: $message")
            recordException(e)
        }
    } catch (_: Exception) {
        // Hlásenie chyby nesmie samo zhodiť appku.
    }
}

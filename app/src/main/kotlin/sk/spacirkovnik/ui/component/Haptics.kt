package sk.spacirkovnik.ui.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Krátke vibrácie počas hry. Hráč má telefón často v ruke alebo vo vrecku a díva sa okolo seba —
 * vibrácia mu povie „si na mieste" alebo „správne" bez pozerania na displej.
 * Telefón bez vibrácie (alebo s chybou) sa jednoducho nič nestane.
 */
internal object Haptics {

    /** Dva krátke pulzy: hráč došiel k cieľu. */
    fun arrived(context: Context) = vibrate(context, longArrayOf(0, 150, 100, 150))

    /** Jeden krátky pulz: správna odpoveď. */
    fun correct(context: Context) = vibrate(context, longArrayOf(0, 60))

    private fun vibrate(context: Context, timings: LongArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }
}

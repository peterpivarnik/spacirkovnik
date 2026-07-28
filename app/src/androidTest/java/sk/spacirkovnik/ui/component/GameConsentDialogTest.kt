package sk.spacirkovnik.ui.component

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import sk.spacirkovnik.model.GameConsent

/**
 * Dialóg súhlasu musí zostať ovládateľný aj vtedy, keď sa text nezmestí na obrazovku — inak sa
 * hráč nedostane k tlačidlu „Súhlasím a pokračovať" a špacírku nespustí.
 *
 * Test beží v konfigurácii zariadenia, na ktorom je spustený. Kritické kombinácie sa dajú
 * odsimulovať pred spustením:
 *
 *     adb shell wm size 2400x1080 && adb shell wm density 420   # na šírku
 *     adb shell wm size 720x1280 && adb shell wm density 320    # malý telefón
 *     adb shell settings put system font_scale 2.0              # zväčšené písmo
 *     adb shell wm size reset && adb shell wm density reset && adb shell settings put system font_scale 1.0
 *
 * Mierka písma sa musí meniť takto systémovo — dialóg má vlastné okno a to si dosadí hustotu
 * z konfigurácie, takže prekrytie LocalDensity v teste sa doň nepremietne.
 */
@RunWith(AndroidJUnit4::class)
class GameConsentDialogTest {

    @get:Rule
    val rule = createComposeRule()

    /** Skutočný súhlas z game-data/catalog/games-info.json. */
    private val realConsent = GameConsent(
        version = 1,
        title = "Súťaž o ceny",
        summary = "Prejdením tejto špacírky sa zapájaš do súťaže o všetky špacírky od " +
            "Špacírkovníka. Použijeme tvoj e-mail a v prípade výhry zverejníme tvoje meno na " +
            "sociálnych sieťach. Podrobnosti v celých podmienkach.",
        organizer = "Špacírkovník (Ing. Peter Pivarník)",
        url = "https://spacirkovnik.sk/sutaz-strieborna-podkova/",
        required = true,
    )

    @Test
    fun actionsStayReachableRegardlessOfTextLength() {
        val variants = listOf(
            "bežný súhlas" to realConsent,
            "2× dlhší" to realConsent.copy(summary = (realConsent.summary + " ").repeat(2).trim()),
            "10× dlhší" to realConsent.copy(summary = (realConsent.summary + " ").repeat(10).trim()),
        )

        var consent by mutableStateOf(variants.first().second)
        var generation by mutableStateOf(0)
        var config = ""

        rule.setContent {
            val density = LocalDensity.current
            val size = LocalWindowInfo.current.containerSize
            config = "${(size.width / density.density).toInt()}×" +
                "${(size.height / density.density).toInt()}dp, " +
                "písmo ${density.fontScale}×"
            key(generation) {
                GameConsentDialog(
                    consent = consent,
                    onAccept = {},
                    onDecline = {},
                    onOpenUrl = {},
                )
            }
        }

        val report = StringBuilder()
        val broken = mutableListOf<String>()

        for ((label, variant) in variants) {
            rule.runOnUiThread {
                consent = variant
                generation++
            }
            rule.waitForIdle()

            // Text sa smie odscrollovať preč, tlačidlá nie — tie musia byť viditeľné hneď.
            val accept = rule.onNodeWithText("Súhlasím a pokračovať")
            val decline = rule.onNodeWithText("Nesúhlasím")
            val acceptOk = runCatching { accept.assertIsDisplayed() }.isSuccess
            val declineOk = runCatching { decline.assertIsDisplayed() }.isSuccess
            // Nadpis môže byť odscrollovaný mimo; podstatné je, že sa k nemu dá dostať.
            val titleReachable = runCatching {
                rule.onNodeWithText("Súťaž o ceny").performScrollTo().assertIsDisplayed()
            }.isSuccess

            // Výška dialógu — krátky súhlas sa nesmie roztiahnuť na celú obrazovku.
            val height = runCatching {
                val b = decline.fetchSemanticsNode().boundsInWindow
                "${(b.bottom / rule.density.density).toInt()}dp"
            }.getOrElse { "?" }

            report.append(
                "$config, $label → tlačidlá ${if (acceptOk && declineOk) "OK" else "MIMO"}, " +
                    "nadpis ${if (titleReachable) "dostupný" else "NEDOSTUPNÝ"}, výška ~$height\n"
            )
            if (!acceptOk || !declineOk) broken.add("$label: tlačidlá mimo obrazovky")
            if (!titleReachable) broken.add("$label: nadpis sa nedá odscrollovať")
        }

        Log.i(TAG, "\n$report")
        println(report)

        if (broken.isNotEmpty()) {
            throw AssertionError("$config\n" + broken.joinToString("\n") + "\n$report")
        }
    }

    private companion object {
        const val TAG = "GameConsentDialogTest"
    }
}

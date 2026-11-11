package sk.spacirkovnik.ui.component

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import sk.spacirkovnik.BuildConfig
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium

/** Dialóg „Účet": e-mail, súhlas s novinkami, odkazy na podmienky a odhlásenie. */
@Composable
internal fun AccountDialog(
    userEmail: String?,
    marketingConsent: Boolean,
    onMarketingConsentChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        // AlertDialog si podklad inak berie z témy, ktorá sleduje systémový
        // tmavý režim — a natvrdo tmavé texty appky by na ňom zmizli.
        containerColor = CardBg,
        titleContentColor = TextDark,
        textContentColor = TextMedium,
        title = { Text("Účet") },
        text = {
            Column {
                userEmail?.let { email ->
                    Text(email, fontSize = 14.sp, color = TextMedium)
                    Spacer(Modifier.height(16.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dostávať novinky a akcie e-mailom",
                        fontSize = 14.sp,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = marketingConsent,
                        onCheckedChange = onMarketingConsentChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryButtonText,
                            checkedTrackColor = PrimaryButton,
                            uncheckedThumbColor = TextMedium,
                            uncheckedTrackColor = CardBg,
                            uncheckedBorderColor = TextMedium,
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Obchodné podmienky",
                    fontSize = 13.sp,
                    color = Amber,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://spacirkovnik.sk/obchodne-podmienky/".toUri()
                            )
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Zásady ochrany osobných údajov",
                    fontSize = 13.sp,
                    color = Amber,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://spacirkovnik.sk/zasady-ochrany-osobnych-udajov/".toUri()
                            )
                        )
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = appVersionLabel(),
                    fontSize = 12.sp,
                    color = TextMedium.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Príbehy a ilustrácie vznikajú s pomocou umelej inteligencie, trasy overujeme v teréne.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = TextMedium.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSignOut) {
                Text("Odhlásiť sa", color = PrimaryButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavrieť", color = TextMedium)
            }
        }
    )
}

/** „Verzia 13 · 9. 11. 2026" vo vydaní; vo vývojovom builde „Verzia 14-SNAPSHOT · vývojová". */
private fun appVersionLabel(): String {
    val date = BuildConfig.BUILD_DATE.ifEmpty { "vývojová" }
    return "Verzia ${BuildConfig.VERSION_NAME} · $date"
}

/**
 * Nudge po prihlásení pre používateľov, ktorí marketing nemajú zapnutý. Pýta sa, ak ešte
 * nerozhodli (žiadny záznam) alebo ak odmietli pred viac než 30 dňami (cooldown – druhá šanca
 * bez otravovania). Po „Áno" sa už nepýta.
 */
internal fun shouldAskMarketingConsent(
    marketingConsent: Boolean,
    marketingConsentTimestamp: Long?,
    nowMs: Long = System.currentTimeMillis(),
): Boolean {
    val cooldownMs = 30L * 24 * 60 * 60 * 1000
    return !marketingConsent &&
        (marketingConsentTimestamp == null || nowMs - marketingConsentTimestamp > cooldownMs)
}

/** Otázka, či chce hráč dostávať e-maily o nových špacírkach a akciách. */
@Composable
internal fun MarketingConsentDialog(
    onAnswer: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextDark,
        textContentColor = TextMedium,
        title = { Text("Novinky a akcie") },
        text = {
            Text(
                "Chceš dostávať e-maily o nových špacírkach a akciách? " +
                    "Súhlas môžeš kedykoľvek odvolať v nastaveniach účtu."
            )
        },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) {
                Text("Áno, chcem", color = PrimaryButton)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) {
                Text("Nie, ďakujem", color = TextMedium)
            }
        }
    )
}

package sk.spacirkovnik.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import sk.spacirkovnik.model.GameConsent
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark

/**
 * Consent dialog shown before a game that requires it. Shows the short summary (informed
 * consent), an optional link to the full terms, and Accept / Decline actions.
 *
 * Built as a custom [Dialog] (not AlertDialog) so the three buttons sit tightly together.
 */
@Composable
fun GameConsentDialog(
    consent: GameConsent,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDecline) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = consent.title ?: "Podmienky účasti",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                )
                consent.summary?.let {
                    Text(text = it, fontSize = 15.sp, lineHeight = 21.sp, color = TextDark)
                }
                consent.organizer?.let {
                    Text(
                        text = "Organizátor: $it",
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.7f),
                    )
                }

                // The three actions, grouped tightly together.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    consent.url?.let { url ->
                        OutlinedButton(
                            onClick = { onOpenUrl(url) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, PrimaryButton),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryButton),
                        ) {
                            Text("Prečítať celé podmienky", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButton,
                            contentColor = PrimaryButtonText,
                        ),
                    ) {
                        Text("Súhlasím a pokračovať", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Nesúhlasím", color = TextDark.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

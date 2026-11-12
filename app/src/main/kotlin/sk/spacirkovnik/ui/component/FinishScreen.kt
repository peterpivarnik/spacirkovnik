package sk.spacirkovnik.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import sk.spacirkovnik.R
import sk.spacirkovnik.model.FinishSummary
import sk.spacirkovnik.model.formatDuration
import sk.spacirkovnik.model.slovakPlural
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnDark

/**
 * Záverečná obrazovka po dohraní špacírky: úvodný obrázok špacírky s pohárom, pochvala, čo všetko
 * hráči zvládli, a malé logo ako podpis (keď si to niekto odfotí a pošle ďalej).
 */
@Composable
internal fun FinishScreen(summary: FinishSummary, onDone: () -> Unit) {
    // Pohár „vyskočí" — krátka pružná animácia pri otvorení.
    val trophyScale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        trophyScale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        if (summary.imageUrl != null) {
            // Obrázok špacírky a pohár v krúžku prekrývajúci jeho spodný okraj.
            Box(contentAlignment = Alignment.BottomCenter) {
                AsyncImage(
                    model = summary.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp + TROPHY_OVERLAP)
                        .padding(bottom = TROPHY_OVERLAP)
                        .clip(RoundedCornerShape(16.dp))
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(trophyScale.value)
                        .size(TROPHY_OVERLAP * 2)
                        .shadow(8.dp, CircleShape)
                        .background(CardBg, CircleShape)
                ) {
                    Text(text = "🏆", fontSize = 52.sp)
                }
            }
        } else {
            Text(text = "🏆", fontSize = 96.sp, modifier = Modifier.scale(trophyScale.value))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Dokázali ste to!",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = TextOnDark,
            textAlign = TextAlign.Center,
        )
        if (summary.title.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Špacírka „${summary.title}“ je za vami.",
                fontSize = 17.sp,
                color = TextOnDark.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (summary.stops > 0) {
                StatCard(
                    value = summary.stops.toString(),
                    label = slovakPlural(summary.stops, "zastávka", "zastávky", "zastávok"),
                    modifier = Modifier.weight(1f),
                )
            }
            if (summary.questions > 0) {
                StatCard(
                    value = summary.questions.toString(),
                    label = slovakPlural(summary.questions, "vyriešená úloha", "vyriešené úlohy", "vyriešených úloh"),
                    modifier = Modifier.weight(1f),
                )
            }
            summary.durationMinutes?.let {
                StatCard(value = formatDuration(it), label = "na ceste", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton),
        ) {
            Text(
                text = "Späť na zoznam špacírok",
                color = PrimaryButtonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(28.dp))
        AsyncImage(
            model = R.drawable.logo,
            contentDescription = "Špacírkovník",
            modifier = Modifier
                .size(110.dp)
                .alpha(0.9f)
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** O koľko pohár prečnieva pod obrázok (polovica priemeru krúžku). */
private val TROPHY_OVERLAP = 44.dp

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = TextMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

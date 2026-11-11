package sk.spacirkovnik.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import sk.spacirkovnik.model.GameAccess
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.PurchaseButton
import sk.spacirkovnik.ui.theme.SecondaryOutlineButton
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.viewmodel.GameListViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel.DownloadStatus

/** Karta špacírky v zozname. Čo ukáže, určuje [access] — sama o odomknutí nerozhoduje. */
@Composable
internal fun GameCard(
    gameWithStatus: GameListViewModel.GameWithStatus,
    access: GameAccess,
    isExpanded: Boolean,
    isPurchasing: Boolean,
    price: String?,
    originalPrice: String?,
    hasSavedProgress: Boolean,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    onPlayFresh: () -> Unit,
    onDownload: () -> Unit,
    onPurchase: () -> Unit,
    onSignInRequest: () -> Unit
) {
    val info = gameWithStatus.info
    val isUnlocked = access.isUnlocked
    val isLocked = !isUnlocked
    // Stmavenie je hlavný signál „túto špacírku ešte nemáš" — drží sa preto obsahu karty
    // (obrázok, názov, popis) pri každej zamknutej špacírke. Prvky, ktoré nesú ponuku — cena,
    // košík, štítok ZADARMO, tlačidlá — ostávajú v plnej sýtosti, nech je vidieť, čo sa s tým dá
    // spraviť. Pri „ide sa získať hneď" (kúpa / prihlásenie) to navyše podčiarkne jantárový rám.
    val isOffer = access.isOffer
    val dimmed = isLocked
    val dimModifier = if (dimmed) Modifier.alpha(0.5f) else Modifier
    val showSignInGate = access.needsSignIn
    val showPurchaseButton = access == GameAccess.BUY

    // Auto-download unlocked games in the background (just the small game JSON; images stream
    // on demand during play), so the player sees "Prejsť" directly without a manual "Stiahnuť"
    // step. If it fails (e.g. offline) the status falls back to NOT_DOWNLOADED and a
    // "Skúsiť znova" button is shown instead.
    var autoDownloadTried by remember(info.id) { mutableStateOf(false) }
    LaunchedEffect(info.id, isUnlocked) {
        if (isUnlocked && gameWithStatus.status == DownloadStatus.NOT_DOWNLOADED) {
            autoDownloadTried = true
            onDownload()
        }
    }

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (isLocked) Modifier.border(
                    width = 1.dp,
                    color = if (isOffer) Amber.copy(alpha = 0.45f) else TextMedium.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dimmed) CardBg.copy(alpha = 0.5f) else CardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Compact header — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!info.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = info.imageUrl,
                    contentDescription = info.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(dimModifier)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Amber.copy(alpha = 0.2f))
                        .then(dimModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .then(dimModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = info.title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (access) {
                // Purchasable → price + shopping cart (you can buy it now); free after sign-in →
                // "ZADARMO" badge; coming soon → clock; any other locked state → padlock. The
                // collapsed row alone should say what it would take to unlock the špacírka.
                GameAccess.BUY, GameAccess.SIGN_IN_TO_BUY -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ceny sa sťahujú až po prihlásení, takže pri SIGN_IN_TO_BUY cena nie je.
                        if (access == GameAccess.BUY && !price.isNullOrEmpty()) {
                            Text(
                                text = price,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Amber,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Dá sa kúpiť",
                            tint = Amber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                GameAccess.FREE_AFTER_SIGN_IN -> {
                    Text(
                        text = "ZADARMO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurchaseButton,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PurchaseButton.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                GameAccess.COMING_SOON -> {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Čoskoro k dispozícii",
                        tint = TextMedium,
                        modifier = Modifier.size(18.dp)
                    )
                }
                GameAccess.LOCKED -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextMedium,
                        modifier = Modifier.size(18.dp)
                    )
                }
                GameAccess.UNLOCKED -> {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMedium,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Expanded detail section
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dimModifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = info.description,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        color = TextMedium,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!info.imageUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        AsyncImage(
                            model = info.imageUrl,
                            contentDescription = info.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 1.dp,
                    color = TextMedium.copy(alpha = 0.2f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dimModifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (info.region != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = info.region, fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                        }
                    }
                    if (info.estimatedDurationMinutes != null || info.distanceKm != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (info.estimatedDurationMinutes != null) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${info.estimatedDurationMinutes} min", fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                            }
                            if (info.estimatedDurationMinutes != null && info.distanceKm != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "•", fontSize = 12.sp, color = TextMedium)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            if (info.distanceKm != null) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${info.distanceKm} km", fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                            }
                        }
                    }
                    if (info.startName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = info.startName, fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                        }
                    }
                    if (info.endName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsScore, contentDescription = null, tint = TextMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = info.endName, fontSize = 12.sp, lineHeight = 14.sp, color = TextMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showSignInGate) {
                    // Bez tohto bloku bola rozbalená karta pre neprihláseného prázdna — nedozvedel
                    // sa, že špacírku odomkne prihlásenie.
                    val isFree = access == GameAccess.FREE_AFTER_SIGN_IN
                    Button(
                        onClick = onSignInRequest,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        // Zelená ostáva vyhradená pre skutočnú kúpu, toto tlačidlo len prihlasuje.
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                    ) {
                        AutoSizeText(
                            text = if (isFree) "Prihlásiť sa a hrať zadarmo" else "Prihlásiť sa",
                            color = PrimaryButtonText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        // Bez prihlásenia nevieme, či špacírku hráč náhodou už nekúpil (nový telefón,
                        // reinštalácia), takže tlačidlo nič netvrdí o kúpe a text pokrýva oba prípady.
                        text = if (isFree)
                            "Táto špacírka je zadarmo — stačí sa prihlásiť."
                        else
                            "Zakúpené špacírky sa ti po prihlásení odomknú. " +
                                "Ak ju ešte nemáš, budeš si ju môcť kúpiť.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = TextMedium
                    )
                } else if (showPurchaseButton) {
                    Button(
                        onClick = onPurchase,
                        enabled = !isPurchasing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurchaseButton)
                    ) {
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryButtonText,
                                strokeWidth = 2.dp
                            )
                        } else {
                            PurchaseButtonLabel(price = price, originalPrice = originalPrice)
                        }
                    }
                } else if (isLocked) {
                    Text(
                        text = "Čoskoro k dispozícii",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = TextMedium.copy(alpha = 0.7f)
                    )
                } else {
                    when (gameWithStatus.status) {
                        DownloadStatus.NOT_DOWNLOADED -> {
                            if (autoDownloadTried) {
                                // Auto-download failed (e.g. offline) — let the user retry.
                                Button(
                                    onClick = onDownload,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                                ) {
                                    Text("Skúsiť znova", color = PrimaryButtonText, fontSize = 16.sp)
                                }
                            } else {
                                // Auto-download is about to start in the background.
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Sťahujem...", fontSize = 16.sp)
                                }
                            }
                        }
                        DownloadStatus.DOWNLOADING -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sťahujem...", fontSize = 16.sp)
                            }
                        }
                        DownloadStatus.DOWNLOADED -> {
                            when {
                                isCompleted -> {
                                    Button(
                                        onClick = onPlayFresh,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                                    ) {
                                        Text("Prejsť znova", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                hasSavedProgress -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = onPlay,
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                                        ) {
                                            Text("Pokračovať", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = onPlayFresh,
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.5.dp, SecondaryOutlineButton),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = SecondaryOutlineButton
                                            )
                                        ) {
                                            Text("Prejsť od začiatku", fontSize = 16.sp)
                                        }
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = onPlay,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                                    ) {
                                        AutoSizeText("Prejsť", color = PrimaryButtonText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        DownloadStatus.UPDATE_AVAILABLE -> {
                            Button(
                                onClick = onPlay,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                            ) {
                                Text("Prejsť", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Popis tlačidla na kúpu — „Kúpiť špacírku · 4,99 €", pri zľave s preškrtnutou pôvodnou cenou.
 * Text sa v prípade potreby zmenší, aby sa dlhšie sumy zmestili na jeden riadok.
 */
@Composable
private fun PurchaseButtonLabel(price: String?, originalPrice: String?) {
    val prefix = "Kúpiť špacírku"
    if (!price.isNullOrEmpty() && originalPrice != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AutoSizeText(
                text = "$prefix ·",
                modifier = Modifier.alignByBaseline().weight(1f, fill = false),
                color = PrimaryButtonText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = originalPrice,
                modifier = Modifier.alignByBaseline(),
                color = PrimaryButtonText.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textDecoration = TextDecoration.LineThrough
            )
            Text(
                text = price,
                modifier = Modifier.alignByBaseline(),
                color = PrimaryButtonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        AutoSizeText(
            text = if (!price.isNullOrEmpty()) "$prefix · $price" else prefix,
            color = PrimaryButtonText,
            fontWeight = FontWeight.Bold
        )
    }
}

package sk.spacirkovnik.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import sk.spacirkovnik.R
import sk.spacirkovnik.data.GameProgressManager
import sk.spacirkovnik.model.GameStatus
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.AmberLight
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.MainBackground
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.PurchaseButton
import sk.spacirkovnik.ui.theme.SecondaryOutlineButton
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnBeige
import sk.spacirkovnik.ui.theme.TextOnBeigeSecondary
import sk.spacirkovnik.viewmodel.AuthViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel.DownloadStatus
import sk.spacirkovnik.viewmodel.PurchaseViewModel

@Composable
fun GameListScreen(
    onGameSelected: (String, String?) -> Unit,
    gameListViewModel: GameListViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    purchaseViewModel: PurchaseViewModel = viewModel()
) {
    val state by gameListViewModel.state
    val authState by authViewModel.state
    val purchaseState by purchaseViewModel.state
    val activity = LocalActivity.current
    val context = LocalContext.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    var expandedGameId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleSignInResult(result.data)
    }

    LaunchedEffect(authState.error) {
        val error = authState.error
        if (error != null) {
            scope.launch { snackbarHostState.showSnackbar("Prihlásenie sa nepodarilo.") }
            authViewModel.clearError()
        }
    }

    LaunchedEffect(purchaseState.justPurchasedGameId) {
        val gameId = purchaseState.justPurchasedGameId ?: return@LaunchedEffect
        authViewModel.grantActivation(gameId)
        purchaseViewModel.clearPurchased()
        snackbarHostState.showSnackbar("Hra bola úspešne zakúpená!")
    }

    LaunchedEffect(purchaseState.error) {
        val error = purchaseState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        purchaseViewModel.clearError()
    }

    LaunchedEffect(authState.isSignedIn) {
        gameListViewModel.refresh()
    }

    LaunchedEffect(authState.testGames) {
        gameListViewModel.setTestGames(authState.testGames)
    }

    LaunchedEffect(state.games) {
        val purchasableIds = state.games
            .filter { it.info.status == GameStatus.PURCHASABLE }
            .map { it.info.googlePlayProductId ?: it.info.id }
        if (purchasableIds.isNotEmpty()) purchaseViewModel.loadProductPrices(purchasableIds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(16.dp)
    ) {
        when {
            state.loading || (authState.isSignedIn && authState.loading) -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Amber
                )
            }
            state.error != null -> {
                Text(
                    text = state.error!!,
                    color = AmberLight,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 4.dp)
                    ) {
                        AsyncImage(
                            model = R.drawable.logo,
                            contentDescription = "Spacirkovnik logo",
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Špacírkovník",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextOnBeige,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vyber si dobrodružstvo",
                                fontSize = 14.sp,
                                color = TextOnBeigeSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(56.dp)
                        ) {
                            if (authState.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Amber,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = {
                                    if (authState.isSignedIn) showSignOutDialog = true
                                    else signInLauncher.launch(authViewModel.getSignInIntent(context))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = if (authState.isSignedIn) "Odhlásiť sa" else "Prihlásiť sa",
                                        tint = if (authState.isSignedIn) Amber else TextOnBeigeSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                if (authState.isSignedIn) {
                                    val firstName = authState.userName?.substringBefore(" ") ?: ""
                                    if (firstName.isNotEmpty()) {
                                        Text(
                                            text = firstName,
                                            fontSize = 11.sp,
                                            color = TextOnBeigeSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.offline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TextMedium.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = TextOnBeigeSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline režim — zobrazujú sa uložené hry",
                                fontSize = 12.sp,
                                color = TextOnBeigeSecondary
                            )
                        }
                    }

                    if (showSignOutDialog) {
                        AlertDialog(
                            onDismissRequest = { showSignOutDialog = false },
                            title = { Text("Odhlásiť sa") },
                            text = { Text("Naozaj sa chceš odhlásiť?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showSignOutDialog = false
                                    authViewModel.signOut()
                                }) {
                                    Text("Odhlásiť sa")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSignOutDialog = false }) {
                                    Text("Zrušiť")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progressManager = remember { GameProgressManager(context) }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.games) { gameWithStatus ->
                            val gameId = gameWithStatus.info.id
                            val productId = gameWithStatus.info.googlePlayProductId ?: gameId
                            val isExpanded = expandedGameId == gameId
                            val isActivated = authViewModel.isGameActivated(gameId)
                            val isTestGame = authViewModel.isTestGame(gameId)
                            val hasSavedProgress = progressManager.hasProgress(gameId)
                            val isCompleted = progressManager.isCompleted(gameId)
                            GameCard(
                                gameWithStatus = gameWithStatus,
                                isSignedIn = authState.isSignedIn,
                                isActivated = isActivated,
                                isTestGame = isTestGame,
                                isExpanded = isExpanded,
                                isPurchasing = purchaseState.purchasingGameId == gameId,
                                price = purchaseState.productPrices[productId],
                                hasSavedProgress = hasSavedProgress,
                                isCompleted = isCompleted,
                                onToggle = {
                                    expandedGameId = if (isExpanded) null else gameId
                                },
                                onPlay = { onGameSelected(gameId, gameWithStatus.info.colorHex) },
                                onPlayFresh = {
                                    progressManager.clearProgress(gameId)
                                    progressManager.clearCompleted(gameId)
                                    onGameSelected(gameId, gameWithStatus.info.colorHex)
                                },
                                onDownload = { gameListViewModel.downloadGame(gameId) },
                                onPurchase = {
                                    if (!authState.isSignedIn) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Pre kúpu hry sa najprv prihláste.")
                                        }
                                    } else if (activity != null) {
                                        purchaseViewModel.purchaseGame(gameId, productId, activity)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GameCard(
    gameWithStatus: GameListViewModel.GameWithStatus,
    isSignedIn: Boolean,
    isActivated: Boolean,
    isTestGame: Boolean,
    isExpanded: Boolean,
    isPurchasing: Boolean,
    price: String?,
    hasSavedProgress: Boolean,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    onPlayFresh: () -> Unit,
    onDownload: () -> Unit,
    onPurchase: () -> Unit
) {
    val info = gameWithStatus.info
    val isUnlocked = info.status == GameStatus.ACTIVE
            || (info.status == GameStatus.FREE_WITH_LOGIN && isSignedIn)
            || isActivated
            || isTestGame
    val isLocked = !isUnlocked
    val showPurchaseButton = isLocked && info.status == GameStatus.PURCHASABLE

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (isLocked) Modifier.border(
                    width = 1.dp,
                    color = TextMedium.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) CardBg.copy(alpha = 0.5f) else CardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Compact header — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .then(if (isLocked) Modifier.alpha(0.5f) else Modifier),
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
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Amber.copy(alpha = 0.2f)),
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
                    .defaultMinSize(minHeight = 48.dp),
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

            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextMedium,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMedium,
                    modifier = Modifier.size(24.dp)
                )
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
                        .then(if (isLocked) Modifier.alpha(0.5f) else Modifier),
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
                        .then(if (isLocked) Modifier.alpha(0.5f) else Modifier),
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

                if (showPurchaseButton) {
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
                            Text(
                                text = if (!price.isNullOrEmpty()) "Kúpiť hru · $price" else "Kúpiť hru",
                                color = PrimaryButtonText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                            Button(
                                onClick = onDownload,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                            ) {
                                Text("Stiahnuť", color = PrimaryButtonText, fontSize = 16.sp)
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
                                        Text("Hrať znova", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                            Text("Hrať od začiatku", fontSize = 16.sp)
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
                                        AutoSizeText("Hrať", color = PrimaryButtonText, fontWeight = FontWeight.Bold)
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
                                Text("Hrať", color = PrimaryButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxFontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 10.sp,
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        fontSize = fontSize,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                val reduced = fontSize.value * 0.9f
                fontSize = if (reduced < minFontSize.value) minFontSize else reduced.sp
            }
        }
    )
}

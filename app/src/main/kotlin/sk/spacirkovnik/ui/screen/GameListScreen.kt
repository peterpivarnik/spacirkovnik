package sk.spacirkovnik.ui.screen

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import sk.spacirkovnik.R
import sk.spacirkovnik.data.ConsentManager
import sk.spacirkovnik.data.GameProgressManager
import sk.spacirkovnik.model.GameInfo
import sk.spacirkovnik.model.GameStatus
import sk.spacirkovnik.model.resolveGameAccess
import sk.spacirkovnik.ui.component.AccountDialog
import sk.spacirkovnik.ui.component.AuthBottomSheet
import sk.spacirkovnik.ui.component.GameCard
import sk.spacirkovnik.ui.component.GameConsentDialog
import sk.spacirkovnik.ui.component.MarketingConsentDialog
import sk.spacirkovnik.ui.component.shouldAskMarketingConsent
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.AmberLight
import sk.spacirkovnik.ui.theme.MainBackground
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextMedium
import sk.spacirkovnik.ui.theme.TextOnBeige
import sk.spacirkovnik.ui.theme.TextOnBeigeSecondary
import sk.spacirkovnik.viewmodel.AuthViewModel
import sk.spacirkovnik.viewmodel.GameListViewModel
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
    var showAuthSheet by remember { mutableStateOf(false) }
    // Špacírka, ktorú chcel hráč odomknúť prihlásením — po úspešnom prihlásení v nej pokračujeme,
    // nech sa nemusí preklikávať späť na kartu.
    var pendingUnlockGameId by remember { mutableStateOf<String?>(null) }
    var marketingPromptDismissed by remember { mutableStateOf(false) }
    var expandedGameId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val consentManager = remember { ConsentManager(context) }
    var consentRequest by remember { mutableStateOf<ConsentRequest?>(null) }
    // Špacírka so súhlasom, kvôli ktorej sa hráč práve prihlasuje: panel ukáže aj podmienky
    // a po prihlásení súhlas zapíšeme — prihlásenie a súhlas sú pre hráča jeden krok.
    var signInConsent by remember { mutableStateOf<ConsentRequest?>(null) }
    val progressManager = remember { GameProgressManager(context) }

    fun needsConsent(info: GameInfo): Boolean {
        val consent = info.consent ?: return false
        return !authViewModel.hasConsent(info.id, consent.version) &&
            consentManager.acceptedVersion(info.id) < consent.version
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleSignInResult(result.data)
    }

    LaunchedEffect(authState.error, showAuthSheet) {
        // Keď je prihlasovací panel otvorený, chybu zobrazí samotný panel.
        // Snackbar použijeme len pre chyby mimo panela (napr. Google flow zvonku).
        if (authState.error != null && !showAuthSheet) {
            scope.launch { snackbarHostState.showSnackbar("Prihlásenie sa nepodarilo.") }
            authViewModel.clearError()
        }
    }

    // Aktivácie zapísané serverom (nákup aj nákupy obnovené na pozadí) prenesieme do stavu
    // prihlásenia, aby sa karta hneď odomkla bez opätovného načítania z Firebase.
    LaunchedEffect(purchaseState.activatedGameIds) {
        if (purchaseState.activatedGameIds.isEmpty()) return@LaunchedEffect
        purchaseState.activatedGameIds.forEach { authViewModel.grantActivation(it) }
        purchaseViewModel.clearActivated()
    }

    LaunchedEffect(purchaseState.justPurchasedGameId) {
        if (purchaseState.justPurchasedGameId == null) return@LaunchedEffect
        purchaseViewModel.clearPurchased()
        snackbarHostState.showSnackbar("Špacírka bola úspešne zakúpená!")
    }

    // Pri každom návrate do appky dohľadáme nepotvrdené nákupy (napr. odložená platba, ktorá
    // sa medzitým dokončila) — Google ich inak po 3 dňoch automaticky vráti.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        purchaseViewModel.syncPurchases()
    }

    // Zlyhané stiahnutie jednej špacírky: krátka hláška, zoznam ostáva a karta ponúkne „Skúsiť znova".
    LaunchedEffect(state.downloadError) {
        val error = state.downloadError ?: return@LaunchedEffect
        gameListViewModel.clearDownloadError()
        snackbarHostState.showSnackbar(error)
    }

    LaunchedEffect(purchaseState.error) {
        val error = purchaseState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        purchaseViewModel.clearError()
    }

    LaunchedEffect(authState.isSignedIn) {
        gameListViewModel.refresh()
        if (!authState.isSignedIn) return@LaunchedEffect
        showAuthSheet = false
        // Súhlas zaškrtnutý v prihlasovacom paneli zapíšeme až teraz, keď poznáme účet.
        // Ak sa zápis nepodarí, súhlas sa hráča opýta znova pri štarte špacírky.
        val req = signInConsent ?: return@LaunchedEffect
        signInConsent = null
        val consent = req.info.consent ?: return@LaunchedEffect
        authViewModel.recordConsent(req.info.id, consent.version) { success ->
            if (success) {
                consentManager.setAccepted(req.info.id, consent.version)
                req.proceed()
            }
        }
    }

    LaunchedEffect(authState.testGames) {
        gameListViewModel.setTestGames(authState.testGames)
    }

    // Po prihlásení sa vráť k špacírke, kvôli ktorej hráč prihlásenie otvoril — rozbalí sa mu tá
    // istá karta, už s cenou a tlačidlom na kúpu. Nákup nespúšťame sami, ten si potvrdí hráč.
    // Čakáme na dotiahnuté aktivácie (authState.loading), inak by sa karta rozbalila ešte
    // v zamknutom stave.
    LaunchedEffect(authState.isSignedIn, authState.loading, state.games) {
        if (!authState.isSignedIn || authState.loading) return@LaunchedEffect
        val gameId = pendingUnlockGameId ?: return@LaunchedEffect
        if (state.games.none { it.info.id == gameId }) return@LaunchedEffect
        pendingUnlockGameId = null
        expandedGameId = gameId
    }

    // Ceny ukazujeme až prihlásenému hráčovi, tak ich dovtedy ani nesťahujeme.
    LaunchedEffect(state.games, authState.isSignedIn) {
        if (!authState.isSignedIn) return@LaunchedEffect
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
                            modifier = Modifier.widthIn(min = 56.dp)
                        ) {
                            when {
                                authState.loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Amber,
                                        strokeWidth = 2.dp
                                    )
                                }
                                authState.isSignedIn -> {
                                    IconButton(onClick = { showSignOutDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Účet",
                                            tint = Amber,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
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
                                else -> {
                                    // Pre neprihláseného je prihlásenie hlavná akcia — nie zašedená
                                    // ikonka bez popisu, ale viditeľné tlačidlo.
                                    Button(
                                        onClick = {
                                            authViewModel.clearError()
                                            showAuthSheet = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton)
                                    ) {
                                        Text(
                                            text = "Prihlásiť",
                                            color = PrimaryButtonText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
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
                                text = "Offline režim — zobrazujú sa uložené špacírky",
                                fontSize = 12.sp,
                                color = TextOnBeigeSecondary
                            )
                        }
                    }

                    // Bez prihlásenia nie je z ničoho zrejmé, že ostatné špacírky sa dajú získať —
                    // pruh to povie skôr, než hráč začne klikať po kartách.
                    val hasUnlockableGames = state.games.any {
                        it.info.status == GameStatus.PURCHASABLE ||
                            it.info.status == GameStatus.FREE_WITH_LOGIN
                    }
                    if (!authState.isSignedIn && hasUnlockableGames) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber.copy(alpha = 0.18f))
                                .clickable {
                                    authViewModel.clearError()
                                    showAuthSheet = true
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = TextOnBeige,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prihlás sa a odomkni ďalšie špacírky",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextOnBeige,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (showAuthSheet) {
                        AuthBottomSheet(
                            state = authState,
                            consent = signInConsent?.info?.consent,
                            onDismiss = {
                                showAuthSheet = false
                                pendingUnlockGameId = null
                                signInConsent = null
                                authViewModel.clearError()
                            },
                            onEmailSignIn = { email, password ->
                                authViewModel.signInWithEmail(email, password)
                            },
                            onEmailRegister = { email, password, marketingOptIn ->
                                authViewModel.registerWithEmail(email, password, marketingOptIn)
                            },
                            onPasswordReset = { email ->
                                authViewModel.sendPasswordReset(email)
                            },
                            onGoogleSignIn = {
                                signInLauncher.launch(authViewModel.getSignInIntent(context))
                            }
                        )
                    }

                    if (showSignOutDialog) {
                        AccountDialog(
                            userEmail = authState.userEmail,
                            marketingConsent = authState.marketingConsent,
                            onMarketingConsentChange = { authViewModel.setMarketingConsent(it) },
                            onSignOut = {
                                showSignOutDialog = false
                                authViewModel.signOut()
                            },
                            onDismiss = { showSignOutDialog = false },
                        )
                    }

                    // Na novinky sa pýtame až hráča, ktorý už dohral špacírku — vtedy otázka dáva
                    // zmysel; hneď po prihlásení by len prekážala ceste k hre.
                    val hasFinishedAnyGame = state.games.any { progressManager.isCompleted(it.info.id) }
                    if (authState.isSignedIn &&
                        hasFinishedAnyGame &&
                        authState.marketingConsentLoaded &&
                        shouldAskMarketingConsent(
                            authState.marketingConsent,
                            authState.marketingConsentTimestamp
                        ) &&
                        !marketingPromptDismissed &&
                        !showAuthSheet &&
                        !showSignOutDialog &&
                        consentRequest == null
                    ) {
                        MarketingConsentDialog(
                            onAnswer = { optIn ->
                                authViewModel.setMarketingConsent(optIn)
                                marketingPromptDismissed = true
                            },
                            onDismiss = { marketingPromptDismissed = true },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.games) { gameWithStatus ->
                            val gameId = gameWithStatus.info.id
                            val productId = gameWithStatus.info.googlePlayProductId ?: gameId
                            val isExpanded = expandedGameId == gameId
                            val access = resolveGameAccess(
                                status = gameWithStatus.info.status,
                                isSignedIn = authState.isSignedIn,
                                isActivated = gameId in authState.activatedGames,
                                isTestGame = gameId in authState.testGames,
                            )
                            val hasSavedProgress = progressManager.hasProgress(gameId)
                            val isCompleted = progressManager.isCompleted(gameId)
                            val info = gameWithStatus.info
                            // Consent gate: if the game requires consent and the player hasn't
                            // accepted the current version yet, show the dialog before starting.
                            fun gatedStart(proceed: () -> Unit) {
                                if (!needsConsent(info)) {
                                    proceed()
                                    return
                                }
                                if (!authState.isSignedIn) {
                                    // Prihlásenie aj súhlas v jednom paneli; po nich sa špacírka spustí.
                                    signInConsent = ConsentRequest(info, proceed)
                                    authViewModel.clearError()
                                    showAuthSheet = true
                                    return
                                }
                                consentRequest = ConsentRequest(info, proceed)
                            }
                            GameCard(
                                gameWithStatus = gameWithStatus,
                                access = access,
                                isExpanded = isExpanded,
                                isPurchasing = purchaseState.purchasingGameId == gameId,
                                price = purchaseState.productPrices[productId],
                                originalPrice = purchaseState.productOriginalPrices[productId],
                                hasSavedProgress = hasSavedProgress,
                                isCompleted = isCompleted,
                                onToggle = {
                                    expandedGameId = if (isExpanded) null else gameId
                                },
                                onPlay = { gatedStart { onGameSelected(gameId, info.colorHex) } },
                                onPlayFresh = {
                                    gatedStart {
                                        progressManager.clearProgress(gameId)
                                        progressManager.clearCompleted(gameId)
                                        onGameSelected(gameId, info.colorHex)
                                    }
                                },
                                onDownload = { gameListViewModel.downloadGame(gameId) },
                                onPurchase = {
                                    if (activity != null) {
                                        purchaseViewModel.purchaseGame(gameId, productId, activity)
                                    }
                                },
                                onSignInRequest = {
                                    pendingUnlockGameId = gameId
                                    signInConsent = if (needsConsent(info)) ConsentRequest(info) {} else null
                                    authViewModel.clearError()
                                    showAuthSheet = true
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

        consentRequest?.let { req ->
            val consent = req.info.consent
            if (consent == null) {
                consentRequest = null
            } else {
                GameConsentDialog(
                    consent = consent,
                    onAccept = {
                        authViewModel.recordConsent(req.info.id, consent.version) { success ->
                            if (success) {
                                consentManager.setAccepted(req.info.id, consent.version)
                                val proceed = req.proceed
                                consentRequest = null
                                proceed()
                            } else {
                                // Keep the dialog open so the player can retry.
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Súhlas sa nepodarilo uložiť. Skontroluj pripojenie a skús to znova."
                                    )
                                }
                            }
                        }
                    },
                    onDecline = { consentRequest = null },
                    onOpenUrl = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        }
                    },
                )
            }
        }
    }
}

/** A pending request to show the consent dialog, plus the action to run once accepted. */
private class ConsentRequest(val info: GameInfo, val proceed: () -> Unit)

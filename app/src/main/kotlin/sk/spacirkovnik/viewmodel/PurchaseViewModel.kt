package sk.spacirkovnik.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Nákupy cez Google Play. Appka nákup sama neaktivuje ani nepotvrdzuje — nákupný token pošle
 * Cloud Function `verifyPurchase` (functions/src/index.ts), ktorá ho overí u Googlu, potvrdí
 * a zapíše aktiváciu. Appka sa o výsledku dozvie z odpovede funkcie.
 *
 * Nespracované nákupy (odložená platba dokončená pri zatvorenej appke, pád appky uprostred
 * nákupu, ...) dohľadá [syncPurchases] pri prihlásení a pri návrate do appky.
 */
class PurchaseViewModel(application: Application) : AndroidViewModel(application), PurchasesUpdatedListener {

    private val auth = FirebaseAuth.getInstance()
    // Rovnaký región, v akom sú nasadené funkcie (setGlobalOptions vo functions/src/index.ts).
    private val functions = FirebaseFunctions.getInstance("europe-west1")

    private val billingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val _state = mutableStateOf(PurchaseState())
    val state: State<PurchaseState> = _state

    private var purchasingProductId: String? = null

    /**
     * Prebiehajúce overenia podľa tokenu. Ten istý nákup môže prísť naraz z [onPurchasesUpdated]
     * aj zo [syncPurchases] (zatvorenie platobného dialógu spustí onResume) — server zavoláme raz.
     * Prístup len z hlavného vlákna (viewModelScope), takže netreba zámok.
     */
    private val inFlight = mutableMapOf<String, Deferred<VerifyResult>>()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        // Po prihlásení dohľadáme všetky nákupy na zariadení — aj tie už potvrdené, ktorým
        // chýba aktivácia (napr. staršia verzia appky ju zapísala pod zlé ID hry).
        if (firebaseAuth.currentUser != null) syncPurchases(includeAcknowledged = true)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun loadProductPrices(gameIds: List<String>) {
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            val productList = gameIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val result = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            )
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val prices = mutableMapOf<String, String>()
                val originals = mutableMapOf<String, String>()
                result.productDetailsList?.forEach { details ->
                    val best = bestOffer(details)
                    prices[details.productId] = best?.formattedPrice ?: ""
                    // If the best (cheapest) offer is below the base price, it's a discount —
                    // remember the base price so the UI can show it struck through.
                    val base = details.oneTimePurchaseOfferDetails
                    if (best != null && base != null && best.priceAmountMicros < base.priceAmountMicros) {
                        originals[details.productId] = base.formattedPrice
                    }
                }
                _state.value = _state.value.copy(
                    productPrices = prices,
                    productOriginalPrices = originals
                )
            }
        }
    }

    fun purchaseGame(gameId: String, productId: String, activity: Activity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(purchasingGameId = gameId, error = null)

            if (!ensureConnected()) {
                failPurchase("Nepodarilo sa pripojiť k obchodu.")
                return@launch
            }

            val queryResult = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    ))
                    .build()
            )

            val productDetails = queryResult.productDetailsList?.firstOrNull()
            if (queryResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK
                || productDetails == null
            ) {
                failPurchase("Produkt nebol nájdený v obchode.")
                return@launch
            }

            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    // Use the (cheapest) available offer's token so any active discount is charged.
                    bestOffer(productDetails)?.offerToken?.let { setOfferToken(it) }
                }
                .build()
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            purchasingProductId = productId
            val flowResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
                failPurchase("Nákup sa nepodarilo spustiť.")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> verifyInteractive(purchase)
                        Purchase.PurchaseState.PENDING -> showPending()
                        else -> {}
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Hra už bola kúpená, no nie je aktivovaná — overíme existujúci nákup.
                val productId = purchasingProductId ?: return
                viewModelScope.launch {
                    val purchase = queryOwnedPurchases().find { it.products.contains(productId) }
                    if (purchase != null) verifyInteractive(purchase)
                    else failPurchase(null)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> failPurchase(null)
            else -> {
                Log.w(TAG, "Nákup zlyhal: ${billingResult.responseCode} ${billingResult.debugMessage}")
                failPurchase(billingErrorMessage(billingResult.responseCode))
            }
        }
    }

    /**
     * Dohľadá nespracované nákupy a pošle ich na overenie. Bez toho by sa nákup s odloženou
     * platbou, dokončený pri zatvorenej appke, nikdy nepotvrdil a Google by ho po 3 dňoch vrátil.
     *
     * [includeAcknowledged] = true overí aj už potvrdené nákupy (server je idempotentný) — volá sa
     * pri prihlásení; pri každom návrate do appky stačia nepotvrdené.
     */
    fun syncPurchases(includeAcknowledged: Boolean = false) {
        if (auth.currentUser == null) return
        viewModelScope.launch {
            if (!ensureConnected()) return@launch
            queryOwnedPurchases()
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .filter { includeAcknowledged || !it.isAcknowledged }
                .forEach { purchase ->
                    launch {
                        // Na pozadí chyby neukazujeme (napr. nákup iného účtu na tomto zariadení
                        // by inak hlásil chybu pri každom spustení). Nepotvrdené skúsime znova nabudúce.
                        val result = verify(purchase).await()
                        if (result is VerifyResult.Activated) addActivated(result.gameId)
                    }
                }
        }
    }

    private fun verifyInteractive(purchase: Purchase) {
        viewModelScope.launch {
            when (val result = verify(purchase).await()) {
                is VerifyResult.Activated -> {
                    addActivated(result.gameId)
                    purchasingProductId = null
                    _state.value = _state.value.copy(
                        purchasingGameId = null,
                        justPurchasedGameId = result.gameId
                    )
                }
                VerifyResult.Pending -> showPending()
                is VerifyResult.Failed -> failPurchase(result.message)
            }
        }
    }

    private fun verify(purchase: Purchase): Deferred<VerifyResult> {
        val token = purchase.purchaseToken
        inFlight[token]?.let { return it }
        // LAZY: viewModelScope beží na Main.immediate, takže bez toho by sa telo mohlo vykonať
        // (a v `finally` odstrániť z mapy) ešte predtým, než ho do mapy vložíme.
        val deferred = viewModelScope.async(start = CoroutineStart.LAZY) {
            try {
                callVerifyPurchase(purchase)
            } finally {
                inFlight.remove(token)
            }
        }
        inFlight[token] = deferred
        deferred.start()
        return deferred
    }

    private suspend fun callVerifyPurchase(purchase: Purchase): VerifyResult {
        if (auth.currentUser == null) {
            return VerifyResult.Failed("Pre aktiváciu špacírky sa musíš prihlásiť.")
        }
        val productId = purchase.products.firstOrNull()
            ?: return VerifyResult.Failed("Nákup neobsahuje produkt.")
        return try {
            val response = functions.getHttpsCallable("verifyPurchase")
                .call(mapOf("purchaseToken" to purchase.purchaseToken, "productId" to productId))
                .await()
            val data = response.data as? Map<*, *>
            val gameId = data?.get("gameId") as? String
            when {
                data?.get("status") == "pending" -> VerifyResult.Pending
                data?.get("status") == "activated" && gameId != null -> VerifyResult.Activated(gameId)
                else -> VerifyResult.Failed("Nepodarilo sa overiť nákup. Skús to znova.")
            }
        } catch (e: FirebaseFunctionsException) {
            Log.w(TAG, "verifyPurchase zlyhal: ${e.code} ${e.message}")
            VerifyResult.Failed(
                when (e.code) {
                    // Pri týchto kódoch server posiela zrozumiteľnú slovenskú správu.
                    FirebaseFunctionsException.Code.UNAUTHENTICATED,
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                    FirebaseFunctionsException.Code.NOT_FOUND,
                    FirebaseFunctionsException.Code.PERMISSION_DENIED,
                    FirebaseFunctionsException.Code.FAILED_PRECONDITION,
                    FirebaseFunctionsException.Code.ALREADY_EXISTS -> e.message
                    else -> null
                } ?: OFFLINE_MESSAGE
            )
        } catch (e: Exception) {
            Log.w(TAG, "verifyPurchase zlyhal", e)
            VerifyResult.Failed(OFFLINE_MESSAGE)
        }
    }

    private suspend fun queryOwnedPurchases(): List<Purchase> =
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ).purchasesList

    private fun addActivated(gameId: String) {
        _state.value = _state.value.copy(activatedGameIds = _state.value.activatedGameIds + gameId)
    }

    private fun showPending() {
        purchasingProductId = null
        _state.value = _state.value.copy(
            purchasingGameId = null,
            error = "Platba čaká na dokončenie. Špacírka sa odomkne automaticky, keď platba prejde."
        )
    }

    /** Ukončí rozbehnutý nákup; [message] = null pre zrušenie bez hlášky. */
    /** Hláška pre používateľa podľa kódu z Google Play (kód samotný ide len do logu). */
    private fun billingErrorMessage(responseCode: Int): String = when (responseCode) {
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.NETWORK_ERROR ->
            "Obchod Google Play je teraz nedostupný. Skontroluj pripojenie a skús to znova."
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            "Nákupy nie sú v tomto zariadení dostupné. Skontroluj, či si prihlásený v Obchode Play."
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
            "Túto špacírku sa teraz nedá kúpiť. Skús to neskôr."
        else -> "Nákup sa nepodarilo dokončiť. Skús to znova."
    }

    private fun failPurchase(message: String?) {
        purchasingProductId = null
        _state.value = _state.value.copy(purchasingGameId = null, error = message)
    }

    /**
     * The cheapest available one-time offer for a product — i.e. the active discount if there is
     * one, otherwise the base price. Falls back to the legacy single-offer field for products that
     * don't use the new purchase-options/offers model.
     */
    private fun bestOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? =
        details.oneTimePurchaseOfferDetailsList?.minByOrNull { it.priceAmountMicros }
            ?: details.oneTimePurchaseOfferDetails

    fun clearPurchased() {
        _state.value = _state.value.copy(justPurchasedGameId = null)
    }

    fun clearActivated() {
        _state.value = _state.value.copy(activatedGameIds = emptySet())
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                }
                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        billingClient.endConnection()
    }

    private sealed interface VerifyResult {
        data class Activated(val gameId: String) : VerifyResult
        data object Pending : VerifyResult
        data class Failed(val message: String) : VerifyResult
    }

    data class PurchaseState(
        val productPrices: Map<String, String> = emptyMap(),
        val productOriginalPrices: Map<String, String> = emptyMap(),
        val purchasingGameId: String? = null,
        val justPurchasedGameId: String? = null,
        /** Hry aktivované serverom (nákup aj obnovenie na pozadí) — UI ich prenesie do AuthViewModel. */
        val activatedGameIds: Set<String> = emptySet(),
        val error: String? = null
    )

    private companion object {
        const val TAG = "PurchaseVM"
        const val OFFLINE_MESSAGE =
            "Nákup prebehol, ale nepodarilo sa ho overiť. Skontroluj pripojenie — skúsime to znova automaticky."
    }
}

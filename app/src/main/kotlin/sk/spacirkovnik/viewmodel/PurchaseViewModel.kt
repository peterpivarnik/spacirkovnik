package sk.spacirkovnik.viewmodel

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import sk.spacirkovnik.data.FIREBASE_DATABASE_URL
import java.security.MessageDigest
import kotlin.coroutines.resume

class PurchaseViewModel(application: Application) : AndroidViewModel(application), PurchasesUpdatedListener {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL)

    private val billingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val _state = mutableStateOf(PurchaseState())
    val state: State<PurchaseState> = _state

    private var purchasingGameId: String? = null
    private var purchasingProductId: String? = null

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
                _state.value = _state.value.copy(
                    purchasingGameId = null,
                    error = "Nepodarilo sa pripojiť k obchodu."
                )
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

            if (queryResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK
                || queryResult.productDetailsList.isNullOrEmpty()
            ) {
                _state.value = _state.value.copy(
                    purchasingGameId = null,
                    error = "Produkt nebol nájdený v obchode."
                )
                return@launch
            }

            val productDetails = queryResult.productDetailsList!!.first()
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

            purchasingGameId = gameId
            purchasingProductId = productId
            val flowResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
                purchasingGameId = null
                _state.value = _state.value.copy(
                    purchasingGameId = null,
                    error = "Nákup sa nepodarilo spustiť."
                )
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Game was bought before but not activated — recover existing purchase
                val productId = purchasingProductId ?: return
                viewModelScope.launch {
                    val result = billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                    val purchase = result.purchasesList.find { it.products.contains(productId) }
                    if (purchase != null) handlePurchase(purchase)
                    else {
                        purchasingGameId = null
                        purchasingProductId = null
                        _state.value = _state.value.copy(purchasingGameId = null)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchasingGameId = null
                purchasingProductId = null
                _state.value = _state.value.copy(purchasingGameId = null)
            }
            else -> {
                purchasingGameId = null
                purchasingProductId = null
                _state.value = _state.value.copy(
                    purchasingGameId = null,
                    error = "Nákup zlyhal (${billingResult.responseCode})."
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val gameId = purchasingGameId ?: purchase.products.firstOrNull() ?: return
        viewModelScope.launch {
            if (!purchase.isAcknowledged) {
                val ackResult = billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
                if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    purchasingGameId = null
                    _state.value = _state.value.copy(
                        purchasingGameId = null,
                        error = "Nepodarilo sa potvrdiť nákup."
                    )
                    return@launch
                }
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                purchasingGameId = null
                purchasingProductId = null
                _state.value = _state.value.copy(
                    purchasingGameId = null,
                    error = "Pre aktiváciu hry sa musíš prihlásiť."
                )
                return@launch
            }

            // A Google Play one-time purchase is owned by the device's Google Play account, not by
            // the app (Firebase) account. Bind each purchase to exactly one Firebase user, so a
            // different account on the same device can't claim someone else's purchase.
            val owner = claimPurchase(sha256(purchase.purchaseToken), uid)
            when {
                owner == null -> {
                    purchasingGameId = null
                    purchasingProductId = null
                    _state.value = _state.value.copy(
                        purchasingGameId = null,
                        error = "Nepodarilo sa overiť nákup. Skús to znova."
                    )
                    return@launch
                }
                owner != uid -> {
                    // Purchase already belongs to another account — do not activate it here.
                    purchasingGameId = null
                    purchasingProductId = null
                    _state.value = _state.value.copy(
                        purchasingGameId = null,
                        error = "Túto hru zakúpil iný účet na tomto zariadení. Ak je tvoja, prihlás sa účtom, ktorým bola kúpená."
                    )
                    return@launch
                }
            }

            database.reference
                .child("activations")
                .child(uid)
                .child(gameId)
                .setValue(true)

            purchasingGameId = null
            purchasingProductId = null
            _state.value = _state.value.copy(
                purchasingGameId = null,
                justPurchasedGameId = gameId
            )
        }
    }

    /**
     * Atomically claims [claimKey] for [uid] if it is still unclaimed, and returns the final owner
     * uid (existing owner if already claimed, [uid] if just claimed, or null on error). Used to bind
     * a Google Play purchase token to a single Firebase account.
     */
    private suspend fun claimPurchase(claimKey: String, uid: String): String? =
        suspendCancellableCoroutine { cont ->
            database.reference.child("purchaseClaims").child(claimKey)
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        if (currentData.value == null) {
                            currentData.value = uid
                        }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        snapshot: DataSnapshot?
                    ) {
                        if (error != null || !committed) {
                            cont.resume(null)
                        } else {
                            cont.resume(snapshot?.getValue(String::class.java))
                        }
                    }
                })
        }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    override fun onCleared() {
        billingClient.endConnection()
    }

    data class PurchaseState(
        val productPrices: Map<String, String> = emptyMap(),
        val productOriginalPrices: Map<String, String> = emptyMap(),
        val purchasingGameId: String? = null,
        val justPurchasedGameId: String? = null,
        val error: String? = null
    )
}

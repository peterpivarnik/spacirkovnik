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
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import sk.spacirkovnik.data.FIREBASE_DATABASE_URL
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
                val prices = result.productDetailsList?.associate { details ->
                    details.productId to (details.oneTimePurchaseOfferDetails?.formattedPrice ?: "")
                } ?: emptyMap()
                _state.value = _state.value.copy(productPrices = prices)
            }
        }
    }

    fun purchaseGame(gameId: String, activity: Activity) {
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
                            .setProductId(gameId)
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
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                ))
                .build()

            purchasingGameId = gameId
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
                val gameId = purchasingGameId ?: return
                viewModelScope.launch {
                    val result = billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                    val purchase = result.purchasesList.find { it.products.contains(gameId) }
                    if (purchase != null) handlePurchase(purchase)
                    else {
                        purchasingGameId = null
                        _state.value = _state.value.copy(purchasingGameId = null)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchasingGameId = null
                _state.value = _state.value.copy(purchasingGameId = null)
            }
            else -> {
                purchasingGameId = null
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
            if (uid != null) {
                database.reference
                    .child("activations")
                    .child(uid)
                    .child(gameId)
                    .setValue(true)
            }

            purchasingGameId = null
            _state.value = _state.value.copy(
                purchasingGameId = null,
                justPurchasedGameId = gameId
            )
        }
    }

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
        val purchasingGameId: String? = null,
        val justPurchasedGameId: String? = null,
        val error: String? = null
    )
}

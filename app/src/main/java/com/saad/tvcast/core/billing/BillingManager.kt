package com.saad.tvcast.core.billing

import android.app.Activity
import android.content.Context
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
import com.saad.tvcast.core.common.PremiumEntitlement
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BillingProducts {
    const val MONTHLY = "premium_monthly_placeholder"
    const val YEARLY = "premium_yearly_placeholder"
    const val LIFETIME = "premium_lifetime_placeholder"
    val subscriptions = listOf(MONTHLY, YEARLY)
    val inAppProducts = listOf(LIFETIME)
}

interface BillingManager {
    val entitlement: StateFlow<PremiumEntitlement>
    suspend fun connect(): BillingResult
    suspend fun restorePurchases()
    suspend fun queryProducts(): List<ProductDetails>
    fun launchPurchase(activity: Activity, productDetails: ProductDetails): BillingResult
}

@Singleton
class GooglePlayBillingManager @Inject constructor(
    @ApplicationContext context: Context
) : BillingManager, PurchasesUpdatedListener {
    private val _entitlement = MutableStateFlow(PremiumEntitlement())
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override suspend fun connect(): BillingResult = suspendCancellableCoroutine { continuation ->
        if (billingClient.isReady) {
            continuation.resume(successResult())
            return@suspendCancellableCoroutine
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (continuation.isActive) continuation.resume(result)
            }

            override fun onBillingServiceDisconnected() {
                _entitlement.value = _entitlement.value.copy(message = "Billing service disconnected.")
            }
        })
    }

    override suspend fun restorePurchases() {
        connect()
        queryPurchases(BillingClient.ProductType.SUBS)
        queryPurchases(BillingClient.ProductType.INAPP)
    }

    override suspend fun queryProducts(): List<ProductDetails> {
        connect()
        val subs = queryProductDetails(BillingProducts.subscriptions, BillingClient.ProductType.SUBS)
        val inApps = queryProductDetails(BillingProducts.inAppProducts, BillingClient.ProductType.INAPP)
        return subs + inApps
    }

    override fun launchPurchase(activity: Activity, productDetails: ProductDetails): BillingResult {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        return billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            updateEntitlement(purchases)
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                .forEach { purchase ->
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(params) { restoreMessage(it) }
                }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            restoreMessage(result)
        } else {
            _entitlement.value = _entitlement.value.copy(message = result.debugMessage)
        }
    }

    private suspend fun queryProductDetails(ids: List<String>, type: String): List<ProductDetails> =
        suspendCancellableCoroutine { continuation ->
            val products = ids.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(type)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()
            billingClient.queryProductDetailsAsync(params) { _, result ->
                if (continuation.isActive) continuation.resume(result.productDetailsList.orEmpty())
            }
        }

    private suspend fun queryPurchases(type: String) {
        val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.queryPurchasesAsync(params) { _, purchases ->
                updateEntitlement(purchases)
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private fun updateEntitlement(purchases: List<Purchase>) {
        val active = purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { it.products }
            .toSet()
        val pending = purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PENDING }
            .flatMap { it.products }
            .toSet()
        _entitlement.value = PremiumEntitlement(
            isPremium = active.isNotEmpty(),
            activeProductIds = active,
            pendingProductIds = pending,
            lastCheckedAt = System.currentTimeMillis()
        )
    }

    private fun restoreMessage(result: BillingResult) {
        _entitlement.value = _entitlement.value.copy(message = result.debugMessage)
    }

    private fun successResult(): BillingResult =
        BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
}

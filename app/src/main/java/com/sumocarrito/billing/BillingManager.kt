package com.sumocarrito.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.sumocarrito.data.prefs.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/** IDs a crear en Play Console: suscripción "pro_monthly" ($0.99) y producto único "pro_lifetime" ($4.99). */
object Skus { const val MONTHLY = "pro_monthly"; const val LIFETIME = "pro_lifetime" }

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext ctx: Context,
    private val store: SettingsStore
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isPro = MutableStateFlow(store.isPro)
    val isPro: StateFlow<Boolean> = _isPro
    private val details = mutableMapOf<String, ProductDetails>()

    private val client = BillingClient.newBuilder(ctx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) scope.launch { loadProducts(); refresh() }
            }
            override fun onBillingServiceDisconnected() {}   // se reintenta en el próximo start()
        })
    }

    private suspend fun loadProducts() {
        fun q(id: String, type: String) = QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
        for (t in listOf(BillingClient.ProductType.SUBS to Skus.MONTHLY, BillingClient.ProductType.INAPP to Skus.LIFETIME)) {
            val res = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(q(t.second, t.first))).build())
            res.productDetailsList?.forEach { details[it.productId] = it }
        }
    }

    /** Revalida contra Play; si no hay red conserva el último estado conocido. */
    suspend fun refresh() {
        var pro = false
        var ok = true
        for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
            val r = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build())
            if (r.billingResult.responseCode != BillingClient.BillingResponseCode.OK) { ok = false; continue }
            r.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
                pro = true; acknowledge(p)
            }
        }
        if (ok) setPro(pro)
    }

    fun launch(activity: Activity, sku: String) {
        val d = details[sku] ?: return
        val pb = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
        d.subscriptionOfferDetails?.firstOrNull()?.let { pb.setOfferToken(it.offerToken) }
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pb.build())).build())
    }

    fun priceOf(sku: String): String? =
        details[sku]?.let { it.oneTimePurchaseOfferDetails?.formattedPrice
            ?: it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice }

    override fun onPurchasesUpdated(r: BillingResult, purchases: MutableList<Purchase>?) {
        if (r.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach {
                scope.launch { acknowledge(it); setPro(true) }
            }
        }
    }

    private suspend fun acknowledge(p: Purchase) {
        if (!p.isAcknowledged)
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
    }

    private fun setPro(v: Boolean) { store.isPro = v; _isPro.value = v }
}

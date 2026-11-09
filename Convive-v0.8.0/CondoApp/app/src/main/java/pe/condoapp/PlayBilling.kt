package pe.condoapp

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Suscripción de Convive con Google Play (producto «convive_plan», planes base «mensual» y «anual»,
 * cada uno con una oferta de 2 meses gratis). Google cobra con la cuenta del teléfono al terminar la prueba.
 */
object PlayBilling {
    const val PRODUCT = "convive_plan"

    sealed interface Result {
        /** Google aprobó: la prueba empezó y el cobro queda programado. */
        data class Subscribed(val purchaseToken: String) : Result
        /** La persona cerró la ventana de Google. */
        data object Cancelled : Result
        /** Google Play no está disponible (APK instalado fuera de Play Store o producto aún no creado). */
        data class Unavailable(val reason: String) : Result
        data class Failed(val reason: String) : Result
    }

    /**
     * Abre el pago de Google para [basePlanId] («mensual» o «anual»). [accountId] (id de la cuenta en Convive)
     * viaja oculto en la compra para que el servidor sepa de quién es. [verify] confirma la compra en el servidor;
     * si devuelve false (servidor sin configurar), la app reconoce la compra para que Google no la anule.
     */
    suspend fun subscribe(activity: Activity, basePlanId: String, accountId: String, verify: suspend (String) -> Boolean): Result {
        val updates = CompletableDeferred<Pair<BillingResult, List<Purchase>?>>()
        val client = BillingClient.newBuilder(activity)
            .setListener { r, purchases -> if (!updates.isCompleted) updates.complete(r to purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        try {
            val setup = connect(client)
            if (setup.responseCode != BillingResponseCode.OK) return Result.Unavailable("setup ${setup.responseCode}")

            val query = QueryProductDetailsParams.newBuilder().setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT).setProductType(BillingClient.ProductType.SUBS).build()
                )
            ).build()
            val details = client.queryProductDetails(query)
            val product = details.productDetailsList?.firstOrNull()
                ?: return Result.Unavailable("product ${details.billingResult.responseCode}")
            val offers = product.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == basePlanId }
            // Primero la oferta con prueba gratis; si no existe, el plan base.
            val offer = offers.firstOrNull { o -> o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
                ?: offers.firstOrNull { it.offerId == null }
                ?: offers.firstOrNull()
                ?: return Result.Unavailable("offer")

            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build())
                )
                .setObfuscatedAccountId(accountId)
                .build()
            val launch = client.launchBillingFlow(activity, params)
            when (launch.responseCode) {
                BillingResponseCode.OK -> Unit
                BillingResponseCode.ITEM_ALREADY_OWNED -> return finish(client, owned(client), verify)
                BillingResponseCode.USER_CANCELED -> return Result.Cancelled
                BillingResponseCode.BILLING_UNAVAILABLE, BillingResponseCode.FEATURE_NOT_SUPPORTED,
                BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.DEVELOPER_ERROR,
                BillingResponseCode.ITEM_UNAVAILABLE -> return Result.Unavailable("launch ${launch.responseCode}")
                else -> return Result.Failed("launch ${launch.responseCode}")
            }

            val (r, purchases) = updates.await()
            return when (r.responseCode) {
                BillingResponseCode.OK -> finish(client, purchases?.firstOrNull { PRODUCT in it.products }, verify)
                BillingResponseCode.ITEM_ALREADY_OWNED -> finish(client, owned(client), verify)
                BillingResponseCode.USER_CANCELED -> Result.Cancelled
                else -> Result.Failed("purchase ${r.responseCode}")
            }
        } finally {
            runCatching { client.endConnection() }
        }
    }

    private suspend fun owned(client: BillingClient): Purchase? =
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
            .purchasesList.firstOrNull { PRODUCT in it.products }

    private suspend fun finish(client: BillingClient, p: Purchase?, verify: suspend (String) -> Boolean): Result {
        if (p == null) return Result.Failed("no_purchase")
        val confirmed = runCatching { verify(p.purchaseToken) }.getOrDefault(false)
        if (!confirmed && p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
        return Result.Subscribed(p.purchaseToken)
    }

    private suspend fun connect(client: BillingClient): BillingResult = suspendCancellableCoroutine { cont ->
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (cont.isActive) cont.resume(result)
            }

            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(BillingResult.newBuilder().setResponseCode(BillingResponseCode.SERVICE_DISCONNECTED).build())
            }
        })
    }

    /** Pantalla de Google Play para administrar o cancelar la suscripción. */
    fun manageUrl(packageName: String) =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT&package=$packageName"
}

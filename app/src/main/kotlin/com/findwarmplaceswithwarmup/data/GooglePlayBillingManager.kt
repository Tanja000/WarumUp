package com.findwarmplaceswithwarmup.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GooglePlayBillingManager(
  private val context: Context,
  private val onPurchaseSuccess: (productId: String) -> Unit = {},
) : PurchasesUpdatedListener {

  private var billingClient: BillingClient? = null
  private var isConnected = false
  private val scope = CoroutineScope(Dispatchers.IO)

  init {
    try {
      val pendingParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

      billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingParams)
        .enableAutoServiceReconnection()
        .build()
      startConnection()
    } catch (e: Throwable) {
      Log.w("PlayBilling", "BillingClient initialization: ${e.message}")
    }
  }

  private fun startConnection() {
    billingClient?.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
          isConnected = true
          Log.i("PlayBilling", "Connected to Google Play Billing")
        }
      }

      override fun onBillingServiceDisconnected() {
        isConnected = false
      }
    })
  }

  fun launchPurchase(
    activity: Activity,
    productId: String,
    quantity: Int = 1,
    onFallbackComplete: () -> Unit,
  ) {
    val client = billingClient
    if (client == null || !isConnected) {
      onFallbackComplete()
      return
    }

    scope.launch {
      try {
        val productList = listOf(
          QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
          .setProductList(productList)
          .build()

        val productDetailsResult = client.queryProductDetails(params)
        val details = productDetailsResult.productDetailsList?.firstOrNull()

        if (details != null) {
          val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

          val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

          activity.runOnUiThread {
            client.launchBillingFlow(activity, flowParams)
          }
        } else {
          activity.runOnUiThread {
            onFallbackComplete()
          }
        }
      } catch (e: Throwable) {
        Log.w("PlayBilling", "Error querying/launching billing: ${e.message}")
        activity.runOnUiThread {
          onFallbackComplete()
        }
      }
    }
  }

  fun restorePurchases(onFinished: (Boolean) -> Unit = {}) {
    val client = billingClient ?: return onFinished(false)
    if (!isConnected) return onFinished(false)

    try {
      val params = com.android.billingclient.api.QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP)
        .build()

      client.queryPurchasesAsync(params) { billingResult, purchases ->
        var restored = false
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
          purchases.forEach { purchase ->
            if (purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
              restored = true
              purchase.products.forEach { prodId ->
                onPurchaseSuccess(prodId)
              }
            }
          }
        }
        onFinished(restored)
      }
    } catch (_: Throwable) {
      onFinished(false)
    }
  }

  fun launchCustomAmountPurchase(
    activity: Activity,
    amountEuro: Double,
    onFallbackComplete: () -> Unit,
  ) {
    val cleanAmount = amountEuro.coerceAtLeast(0.50)
    val matchedSku = when {
      abs(cleanAmount - 0.99) < 0.05 -> MonetizationConfig.SKU_DONATION_099
      abs(cleanAmount - 2.50) < 0.30 -> MonetizationConfig.SKU_HOT_CHOCOLATE
      abs(cleanAmount - 5.00) < 0.30 -> MonetizationConfig.SKU_GLUEHWEIN
      abs(cleanAmount - 10.00) < 0.30 -> MonetizationConfig.SKU_DONATION_1000
      else -> MonetizationConfig.SKU_DONATION_UNIT_1EUR
    }
    val quantity = if (matchedSku == MonetizationConfig.SKU_DONATION_UNIT_1EUR) {
      cleanAmount.roundToInt().coerceIn(1, 500)
    } else {
      1
    }
    launchPurchase(
      activity = activity,
      productId = matchedSku,
      quantity = quantity,
      onFallbackComplete = onFallbackComplete,
    )
  }

  override fun onPurchasesUpdated(
    billingResult: BillingResult,
    purchases: List<com.android.billingclient.api.Purchase>?,
  ) {
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      for (purchase in purchases) {
        if (purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
          val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
          billingClient?.consumeAsync(consumeParams) { _, _ -> }
          purchase.products.forEach { prodId ->
            onPurchaseSuccess(prodId)
          }
        }
      }
    }
  }
}

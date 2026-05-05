package com.geminiapps.wifitethering.domain.billing

import com.android.billingclient.api.Purchase

/**
 * Returns true when [purchases] contains a PURCHASED (not pending) item whose
 * product list includes [productId].
 *
 * Intentionally a top-level pure function so it is trivial to unit-test
 * without an Android context or BillingClient.
 */
fun determinePremiumStatus(purchases: List<Purchase>, productId: String): Boolean =
    purchases.any { purchase ->
        purchase.products.contains(productId) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

/**
 * Returns the subset of [purchases] that are in the PURCHASED state but have
 * not yet been acknowledged. These must be acknowledged to avoid automatic
 * refund by Google Play.
 */
fun filterUnacknowledged(purchases: List<Purchase>): List<Purchase> =
    purchases.filter { purchase ->
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
    }

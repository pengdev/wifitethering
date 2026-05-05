package com.geminiapps.wifitethering.domain

import com.android.billingclient.api.Purchase
import com.geminiapps.wifitethering.domain.billing.determinePremiumStatus
import com.geminiapps.wifitethering.domain.billing.filterUnacknowledged
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for pure billing purchase-handling logic extracted into
 * [com.geminiapps.wifitethering.domain.billing].
 *
 * No Android context or BillingClient required — just pure functions.
 */
class PurchaseHandlerTest {

    private val productId = "premium_monthly"

    // --- determinePremiumStatus --------------------------------------------

    @Test
    fun `returns true when matching purchased product found`() {
        val purchase = mockPurchase(listOf(productId), Purchase.PurchaseState.PURCHASED, acked = true)
        assertTrue(determinePremiumStatus(listOf(purchase), productId))
    }

    @Test
    fun `returns false when purchase list is empty`() {
        assertFalse(determinePremiumStatus(emptyList(), productId))
    }

    @Test
    fun `returns false when product ID does not match`() {
        val purchase = mockPurchase(listOf("other_product"), Purchase.PurchaseState.PURCHASED, acked = true)
        assertFalse(determinePremiumStatus(listOf(purchase), productId))
    }

    @Test
    fun `returns false when purchase state is pending`() {
        val purchase = mockPurchase(listOf(productId), Purchase.PurchaseState.PENDING, acked = false)
        assertFalse(determinePremiumStatus(listOf(purchase), productId))
    }

    @Test
    fun `returns true for unacknowledged but purchased product`() {
        val purchase = mockPurchase(listOf(productId), Purchase.PurchaseState.PURCHASED, acked = false)
        assertTrue(determinePremiumStatus(listOf(purchase), productId))
    }

    // --- filterUnacknowledged ----------------------------------------------

    @Test
    fun `filterUnacknowledged returns only purchased-and-unacknowledged items`() {
        val pending       = mockPurchase(listOf(productId), Purchase.PurchaseState.PENDING,   acked = false)
        val purchasedAcked   = mockPurchase(listOf(productId), Purchase.PurchaseState.PURCHASED, acked = true)
        val purchasedUnacked = mockPurchase(listOf(productId), Purchase.PurchaseState.PURCHASED, acked = false)

        val result = filterUnacknowledged(listOf(pending, purchasedAcked, purchasedUnacked))
        assertTrue(result == listOf(purchasedUnacked))
    }

    @Test
    fun `filterUnacknowledged returns empty list when all purchases are acknowledged`() {
        val p = mockPurchase(listOf(productId), Purchase.PurchaseState.PURCHASED, acked = true)
        assertTrue(filterUnacknowledged(listOf(p)).isEmpty())
    }

    // --- helpers -----------------------------------------------------------

    private fun mockPurchase(
        products: List<String>,
        state: Int,
        acked: Boolean,
    ): Purchase = mockk<Purchase> {
        every { this@mockk.products } returns products
        every { purchaseState } returns state
        every { isAcknowledged } returns acked
    }
}

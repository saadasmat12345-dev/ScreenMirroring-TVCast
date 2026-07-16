package com.saad.tvcast

import com.saad.tvcast.core.billing.BillingProducts
import com.saad.tvcast.core.common.PremiumEntitlement
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementTest {
    @Test
    fun activeProductMeansPremium() {
        val entitlement = PremiumEntitlement(
            isPremium = true,
            activeProductIds = setOf(BillingProducts.MONTHLY)
        )
        assertTrue(entitlement.isPremium)
        assertTrue(BillingProducts.MONTHLY in entitlement.activeProductIds)
    }
}

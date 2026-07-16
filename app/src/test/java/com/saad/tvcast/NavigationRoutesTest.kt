package com.saad.tvcast

import com.saad.tvcast.core.navigation.Destination
import com.saad.tvcast.core.navigation.bottomTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {
    @Test
    fun bottomTabsMatchRequiredSections() {
        assertEquals(listOf("home", "browser", "library", "activity", "settings"), bottomTabs.map { it.destination.route })
    }

    @Test
    fun secondaryRoutesExist() {
        assertTrue(Destination.Devices.route.isNotBlank())
        assertTrue(Destination.Mirroring.route.isNotBlank())
        assertTrue(Destination.Premium.route.isNotBlank())
    }
}

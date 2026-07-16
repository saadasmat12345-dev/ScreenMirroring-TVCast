package com.saad.tvcast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.saad.tvcast.core.common.CastConnection
import com.saad.tvcast.core.designsystem.component.ConnectionCard
import com.saad.tvcast.core.designsystem.theme.TVCastTheme
import org.junit.Rule
import org.junit.Test

class ConnectionCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsDisconnectedState() {
        composeRule.setContent {
            TVCastTheme {
                ConnectionCard(
                    connection = CastConnection(),
                    onScan = {},
                    onDisconnect = {},
                    onReconnect = {}
                )
            }
        }

        composeRule.onNodeWithText("Not connected").assertIsDisplayed()
        composeRule.onNodeWithText("Scan for devices").assertIsDisplayed()
    }
}

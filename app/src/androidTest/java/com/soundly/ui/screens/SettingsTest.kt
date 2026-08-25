package com.soundly.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.soundly.ui.theme.SoundlyTheme
import org.junit.Rule
import org.junit.Test

class SettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysTitle() {
        // Start the screen
        composeTestRule.setContent {
            SoundlyTheme {
                SettingsScreen()
            }
        }

        // Verify that the text "Settings" is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}

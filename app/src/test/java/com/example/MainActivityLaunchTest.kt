package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches with clean writing box copy save note and tabs`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("header_spen_status").assertIsDisplayed()
        composeRule.onNodeWithTag("spen_writing_box").assertIsDisplayed()
        composeRule.onNodeWithTag("spen_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("undo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("clear_button").assertIsDisplayed()
        composeRule.onNodeWithTag("convert_button").assertIsDisplayed()
        composeRule.onNodeWithTag("draft_text_field").assertIsDisplayed()
        composeRule.onNodeWithTag("copy_text_button").assertIsDisplayed()
        composeRule.onNodeWithTag("save_note_button").assertIsDisplayed()
        composeRule.onNodeWithTag("add_task_button").assertIsDisplayed()

        // Ensure S PEN ONLY badge and writing box hints remain removed from the writing box
        composeRule.onNodeWithText("S PEN ONLY").assertDoesNotExist()
        composeRule.onNodeWithText("Write with your S Pen").assertDoesNotExist()

        // Test palette toggle opens background & pen color swatches
        composeRule.onNodeWithTag("palette_toggle_button").performClick()
        composeRule.onNodeWithTag("quick_palette_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("pen_swatch_cyan").performClick()
        composeRule.onNodeWithTag("palette_toggle_button").performClick()

        // Enter recognized draft, copy to clipboard, save as a Note, and verify in Notes tab
        composeRule.onNodeWithTag("draft_text_field").performTextInput("شراء Coffee")
        composeRule.onNodeWithTag("copy_text_button").performClick()

        val clipboard = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("شراء Coffee", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        composeRule.onNodeWithTag("save_note_button").performClick()

        // Navigate to Notes tab and confirm the saved note appears
        composeRule.onNodeWithTag("nav_tab_notes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("شراء Coffee").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_list").assertIsDisplayed()

        // Navigate to Settings tab and verify Google Sync, Colors, View Settings, and About sections
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.onNodeWithTag("settings_google_sync_card").assertIsDisplayed()
        composeRule.onNodeWithTag("google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_colors_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_view_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_about_card").performScrollTo().assertIsDisplayed()

        // Switch back to Write tab and verify adding a Task works
        composeRule.onNodeWithTag("nav_tab_write").performClick()
        composeRule.onNodeWithTag("draft_text_field").performTextInput("Call Ahmed")
        composeRule.onNodeWithTag("add_task_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Call Ahmed").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Call Ahmed").assertIsDisplayed()
    }
}

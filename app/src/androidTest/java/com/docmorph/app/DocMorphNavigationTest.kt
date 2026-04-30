package com.docmorph.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration tests that run on a real device or emulator.
 *
 * These verify the full navigation graph and screen composition from
 * [MainActivity] — no PDF file is needed for the Home screen checks.
 *
 * To run:
 *   ./gradlew connectedAndroidTest
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DocMorphNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ─── Home Screen ──────────────────────────────────────────────────────────

    @Test
    fun homeScreen_displaysAppTitle() {
        composeRule
            .onNodeWithText("DocMorph")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysOpenPdfButton() {
        composeRule
            .onNodeWithText("Open PDF")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_emptyState_displaysEmptyMessage() {
        // When there are no recent files the empty state text should be visible
        composeRule
            .onNodeWithText("No PDFs opened yet", substring = true)
            .assertIsDisplayed()
    }

    // ─── Accessibility / content descriptions ─────────────────────────────────

    @Test
    fun homeScreen_openPdfFAB_hasAccessibilityLabel() {
        composeRule
            .onNodeWithContentDescription("Open PDF")
            .assertIsDisplayed()
    }
}

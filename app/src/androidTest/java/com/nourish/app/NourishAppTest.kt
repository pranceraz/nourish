package com.nourish.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.NumberFormat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NourishAppTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun sampleAddUpdatesTodayAndCanBeHiddenInSettings() {
        val format = NumberFormat.getIntegerInstance()
        compose.onNodeWithText("DEMO DATA").assertIsDisplayed()
        compose.onNodeWithText(format.format(1_482)).assertIsDisplayed()

        compose.onNodeWithContentDescription("Quick add meal").performClick()
        compose.onNodeWithText("Quick add").assertIsDisplayed()
        compose.onNodeWithText("Browse sample foods").performClick()
        compose.onNodeWithText("Hummus & crackers").performClick().assertIsSelected()
        compose.onNodeWithText("Add to demo").performClick()
        compose.onNodeWithText(format.format(1_692)).assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
        compose.onNodeWithText("Hummus & crackers").assertIsDisplayed()

        compose.onNodeWithText("Diary").performClick()
        compose.onNodeWithText("Your meal log").assertIsDisplayed()
        compose.onNodeWithText("Insights").performClick()
        compose.onNodeWithText("A view of your week").assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("About").assertIsDisplayed()
        val demoSwitch = compose.onNodeWithContentDescription("Show demo data")
        demoSwitch.performClick().assertIsOff()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("Your day is open").assertIsDisplayed()
        compose.onNodeWithText("DEMO DATA").assertDoesNotExist()

        compose.onNodeWithContentDescription("Quick add meal").performClick()
        compose.onNodeWithText("Demo settings").performClick()
        compose.onNodeWithText("About").assertIsDisplayed()
        demoSwitch.performClick()
    }
}

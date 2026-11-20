package com.nourish.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

class NourishAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tap(text: String) = compose.onNodeWithText(text).performClick()
    private fun field(label: String, value: String) {
        compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value)
    }
    private fun save() = compose.onNodeWithText("Save food").performScrollTo().performClick()

    @Test fun offlineDiary() {
        val phase = InstrumentationRegistry.getArguments().getString("phase")
        if (phase != "reopen") {
            compose.onNodeWithContentDescription("Quick add meal").performClick()
            if (compose.onAllNodesWithText("Create a custom food").fetchSemanticsNodes().isNotEmpty()) tap("Create a custom food")
            field("Food name","Apricot yogurt")
            field("Energy (kcal)","160")
            field("Protein (g)","12")
            field("Carbs (g)","18")
            field("Fat (g)","4")
            field("Servings eaten","1.5")
            save()
            compose.waitUntil(15000) { compose.onAllNodesWithText("240 kcal").fetchSemanticsNodes().isNotEmpty() }
            if (phase == "create") return
            compose.activityRule.scenario.recreate()
        } else tap("Diary")
        compose.onNodeWithText("Apricot yogurt").assertIsDisplayed()
        compose.onNodeWithText("240 kcal").assertIsDisplayed()
        if (phase == "reopen") {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val file = File(instrumentation.targetContext.filesDir,"diary-test.png")
            file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
        }
        tap("Actions")
        tap("Edit")
        field("Servings eaten","2")
        save()
        compose.waitUntil(15000) { compose.onAllNodesWithText("320 kcal").fetchSemanticsNodes().isNotEmpty() }
        tap("Actions")
        tap("Delete")
        tap("Delete entry")
        compose.onNodeWithText("Your meal log").assertIsDisplayed()
    }
}

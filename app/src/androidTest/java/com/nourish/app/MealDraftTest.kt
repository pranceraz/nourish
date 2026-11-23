package com.nourish.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nourish.app.diary.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MealDraftTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun tap(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun findFood(search: String, name: String) {
        compose.onNode(hasSetTextAction() and hasText("Find a USDA food or dish")).performScrollTo().performTextReplacement(search)
        compose.waitUntil(15000) { compose.onAllNodes(hasClickAction() and hasText(name)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasClickAction() and hasText(name)).performScrollTo().performClick()
    }

    @Test fun bothSaveModesPreserveTheSameNutrients() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ctx = inst.targetContext
        val dao = DiaryDatabase.open(ctx).diary()
        val day = LocalDate.now().toString()
        fun today() = runBlocking { dao.entries(day,day).first() }
        val before = today().map { it.id }.toSet()
        for (dish in listOf(false,true)) {
            compose.waitUntil(20000) { compose.onAllNodesWithContentDescription("Quick add meal").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Quick add meal").performClick()
            tap("Add food or dish")
            findFood("goat milk","Goat milk")
            val grams = compose.onNode(hasSetTextAction() and hasText("Grams"))
            compose.onNodeWithContentDescription("Grams for Goat milk").performScrollTo()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(150f) }
            grams.assertTextContains("150")
            grams.performTextReplacement("100")
            findFood("buttermilk","Buttermilk")
            if (dish) {
                tap("Complete dish")
                compose.onNode(hasSetTextAction() and hasText("Dish name")).performScrollTo().performTextReplacement("Two milks")
                compose.onNodeWithText("COMBINED TOTAL").performScrollTo().assertIsDisplayed()
                File(ctx.filesDir,"meal-draft-test.png").outputStream().use {
                    inst.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
                }
            }
            tap("Save reviewed meal")
            compose.waitUntil(15000) { today().count { it.id !in before } == if (dish) 3 else 2 }
        }
        val entries = today().filter { it.id !in before }
        val complete = entries.single { it.label.unit == "Serving" }
        val separate = entries.filter { it.id != complete.id }
        assertEquals("Two milks",complete.label.name)
        assertEquals(112,separate.nutrition().calories)
        assertEquals(separate.nutrition(),listOf(complete).nutrition())
        assertEquals(0,complete.amount("301")!!.compareTo(separate.map { it.amount("301")!! }.reduce { a,b -> a+b }))
        assertNull(complete.amount("315"))
        val store = DiaryPreferences(ctx)
        runBlocking {
            val date = LocalDate.parse(day)
            store.target("301",date,"750")
            store.visible("301",false)
            assertTrue("301" in store.values.first().hidden)
            store.visible("301",true)
            assertEquals("750",store.values.first().target("301",date))
            assertNull(store.values.first().target("301",date.minusDays(1)))
            entries.forEach { dao.delete(it.id) }
        }
    }
}

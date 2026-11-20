package com.nourish.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nourish.app.diary.DiaryDatabase
import com.nourish.app.diary.ServiceError
import com.nourish.app.diary.amount
import com.nourish.app.diary.nutrition
import com.nourish.app.diary.unpackMeal
import com.nourish.app.ui.Destination
import com.nourish.app.ui.NourishApp
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConfirmedChatTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun reviewedDishSurvivesRestartAndRetry() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val fixture = instrumentation.context.assets.open("confirmed-chat-meals.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val account = fixture.getString("account")
        val rows = fixture.getJSONArray("meals")
        val meals = (0 until rows.length()).map { unpackMeal(rows.getJSONObject(it), it.toLong()) }
        val date = LocalDate.parse("2026-06-10")
        val file = "confirmed-chat-${UUID.randomUUID()}"
        fun open() = Room.databaseBuilder(ctx, DiaryDatabase::class.java, file).build()
        val initial = open()
        try {
            runBlocking { assertEquals(1, initial.diary().receiveChat(account, meals.take(1))) }
        } finally { initial.close() }
        val reopened = open()
        try {
            val dao = reopened.diary()
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    NourishApp(date, initiallyShowDemo = false, dao = dao, initialDestination = Destination.Diary)
                }
            }
            compose.waitUntil(20000) { compose.onAllNodesWithText("Morning bowl").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Morning bowl").assertIsDisplayed()
            compose.onNodeWithText("279 kcal").assertIsDisplayed()
            File(ctx.filesDir, "confirmed-chat-test.png").outputStream().use {
                instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            runBlocking {
                val dish = requireNotNull(dao.entry(meals.first().entries.single().id))
                assertTrue(JSONObject(dish.label.details!!).getJSONObject("partial").getBoolean("301"))
                assertEquals("Breakfast", dish.meal)
                dao.delete(dish.id)
                assertEquals(1, dao.receiveChat(account, meals))
                assertEquals(0, dao.receiveChat(account, meals))
                assertNull(dao.entry(dish.id))
                val parts = dao.entries(date.toString(), date.toString()).first()
                assertEquals(2, parts.size)
                assertEquals(parts.nutrition(), listOf(dish).nutrition())
                val granola = parts.single { it.label.name == "Granola" }
                assertEquals(50000L, granola.quantity)
                assertNull(granola.amount("301"))
                assertTrue(granola.label.details!!.contains("GPT estimate from the photo"))
                try {
                    dao.receiveChat("different-account", emptyList())
                    fail("An unrelated account was accepted")
                } catch (_: ServiceError) {
                    assertEquals(account, dao.chatCursor()?.account)
                }
            }
        } finally {
            compose.activityRule.scenario.close()
            reopened.close()
            ctx.deleteDatabase(file)
        }
    }
}

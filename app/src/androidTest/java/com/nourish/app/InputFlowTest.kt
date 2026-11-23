package com.nourish.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.nourish.app.diary.MealImages
import java.io.File
import org.junit.Rule
import org.junit.Test

class InputFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun tap(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
    private fun field(label: String, value: String) =
        compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value)
    private fun waitText(text: String) = compose.waitUntil(20000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun scanRememberSearchAndAttachPhoto() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val inputs = MealImages(ctx)
        val product = MultiFormatWriter().encode("5901234123457", BarcodeFormat.EAN_13, 500, 220)
        val bitmap = Bitmap.createBitmap(500, 220, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            bitmap.setPixel(x,y, if (product[x,y]) Color.BLACK else Color.WHITE)
        }
        val sample = inputs.cameraFile()
        sample.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        // Camera and picker are external processes. Supply deterministic results at that boundary.
        var cancelPicker = true
        val monitor = object : Instrumentation.ActivityMonitor() {
            @Suppress("DEPRECATION")
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                    val destination: Uri = requireNotNull(intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT))
                    ctx.contentResolver.openOutputStream(destination)!!.use { out -> sample.inputStream().use { it.copyTo(out) } }
                    return Instrumentation.ActivityResult(Activity.RESULT_OK,null)
                }
                if (intent.action in listOf(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT, MediaStore.ACTION_PICK_IMAGES,
                    "androidx.activity.result.contract.action.PICK_IMAGES")) {
                    if (cancelPicker) {
                        cancelPicker = false
                        return Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null)
                    }
                    return Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(inputs.cameraUri(sample)))
                }
                return null
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.waitUntil(30000) { compose.onAllNodesWithContentDescription("Quick add meal").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Quick add meal").performClick()
            tap("Photo or barcode")
            tap("Scan package barcode")
            waitText("Package · 05901234123457")
            field("Food name","Test lentils")
            tap("Choose photo")
            compose.onNodeWithText("Food name").assertExists()
            tap("Choose photo")
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Meal photo").fetchSemanticsNodes().isNotEmpty() }
            listOf("Energy (kcal)" to "180", "Protein (g)" to "12", "Carbs (g)" to "30", "Fat (g)" to "1").forEach { (k,v) -> field(k,v) }
            tap("Save food")
            waitText("Test lentils")
            compose.onNodeWithContentDescription("Quick add meal").performClick()
            field("Search saved foods or type barcode", "LENTIL")
            compose.onNode(hasText("Test lentils") and hasClickAction()).assertExists()
            field("Search saved foods or type barcode", "5901234123457")
            tap("Use barcode")
            compose.onNode(hasSetTextAction() and hasText("Test lentils")).assertExists()
            tap("Close")
            compose.onNodeWithText("Actions").performClick()
            compose.onNodeWithText("Edit").performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Meal photo").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Meal photo").performScrollTo().assertIsDisplayed()
            File(ctx.filesDir,"photo-input-test.png").outputStream().use {
                instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            sample.delete()
        }
    }
}

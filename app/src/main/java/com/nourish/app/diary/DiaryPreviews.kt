package com.nourish.app.diary

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nourish.app.ui.theme.NourishTheme
import java.time.LocalDate

private val example = Entry(
    id = "preview", date = "2026-07-03", meal = "Breakfast", foodId = "apricot",
    label = Label("Apricot yogurt", "Serving", "Unspecified", 160000, 12000, 18000, 4000),
    quantity = 1500, created = 0,
)

@Preview(name = "Saved breakfast", widthDp = 360, heightDp = 740)
@Preview(name = "Saved breakfast · dark and large type", widthDp = 390, heightDp = 844,
    fontScale = 1.6f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SavedMealPreview() {
    NourishTheme {
        DiaryScreen(LocalDate.parse(example.date), listOf(example), {}, {}, {}, {}, {})
    }
}

@Preview(name = "Edit portion", widthDp = 390, heightDp = 844)
@Composable
private fun EditPortionPreview() {
    NourishTheme { EntryEditor(example.date, example, emptyList(), {}, {}, {}) }
}

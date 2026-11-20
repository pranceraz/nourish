package com.nourish.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.add.QuickAddPanel
import com.nourish.app.ui.theme.NourishTheme
import com.nourish.app.ui.theme.nourishColors
import com.nourish.app.ui.today.HeroMetric
import com.nourish.app.ui.today.PastelMetricCard
import java.time.LocalDate

private val previewDate: LocalDate = LocalDate.of(2026, 11, 19)

@Preview(name = "Compact", group = "Today", widthDp = 360, heightDp = 740)
@Preview(name = "Large phone", group = "Today", widthDp = 430, heightDp = 890)
@Preview(name = "Dark", group = "Today", widthDp = 390, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", group = "Today", widthDp = 360, heightDp = 780, fontScale = 2f)
@Composable
private fun TodayPreviews() {
    NourishApp(date = previewDate)
}

@Preview(name = "Demo off", group = "Today", widthDp = 360, heightDp = 740)
@Composable
private fun EmptyTodayPreview() {
    NourishApp(date = previewDate, initiallyShowDemo = false)
}

@Preview(name = "Diary", group = "Tabs", widthDp = 360, heightDp = 740)
@Composable
private fun DiaryPreview() {
    NourishApp(date = previewDate, initialDestination = Destination.Diary)
}

@Preview(name = "Insights", group = "Tabs", widthDp = 360, heightDp = 740)
@Composable
private fun InsightsPreview() {
    NourishApp(date = previewDate, initialDestination = Destination.Insights)
}

@Preview(name = "Settings", group = "Tabs", widthDp = 360, heightDp = 740)
@Preview(name = "Settings dark, large text", group = "Tabs", widthDp = 360, heightDp = 780, fontScale = 2f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreviews() {
    NourishApp(date = previewDate, initialDestination = Destination.Settings)
}

@Preview(name = "Entry", group = "Quick add", widthDp = 360, showBackground = true, backgroundColor = 0xFF201E21)
@Composable
private fun QuickAddPreview() {
    SheetPreview(browsing = false)
}

@Preview(name = "Food selection", group = "Quick add", widthDp = 360, showBackground = true, backgroundColor = 0xFF201E21)
@Preview(name = "Food selection at large text", group = "Quick add", widthDp = 360, fontScale = 2f, showBackground = true, backgroundColor = 0xFF201E21)
@Composable
private fun SampleFoodsPreview() {
    SheetPreview(browsing = true)
}

@Composable
private fun SheetPreview(browsing: Boolean) {
    NourishTheme {
        QuickAddPanel(
            showDemo = true,
            browsing = browsing,
            selection = 0,
            onBrowse = {},
            onSelection = {},
            onBack = {},
            onClose = {},
            onAdd = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Hero and macro", group = "Components", widthDp = 390)
@Composable
private fun MetricsPreview() {
    NourishTheme {
        val colors = nourishColors
        Column(Modifier.background(colors.canvas)) {
            HeroMetric(1_482, 2_100)
            PastelMetricCard("Protein", 83, 120, colors.pink, Sticker.Petal, Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

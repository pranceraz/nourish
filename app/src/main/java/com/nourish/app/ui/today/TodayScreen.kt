package com.nourish.app.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nourish.app.model.DemoFood
import com.nourish.app.model.DemoNutrition
import com.nourish.app.model.Nutrition
import com.nourish.app.ui.CitrusSticker
import com.nourish.app.ui.EmptyState
import com.nourish.app.ui.Glyph
import com.nourish.app.ui.Heading
import com.nourish.app.ui.NourishIcon
import com.nourish.app.ui.Sticker
import com.nourish.app.ui.StickerShape
import com.nourish.app.ui.formatted
import com.nourish.app.ui.theme.NourishMotion
import com.nourish.app.ui.theme.NourishShape
import com.nourish.app.ui.theme.NourishType
import com.nourish.app.ui.theme.nourishColors
import com.nourish.app.diary.Preferences
import com.nourish.app.diary.NutrientSummary
import com.nourish.app.diary.Entry
import com.nourish.app.diary.nutrition
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun TodayScreen(
    date: LocalDate,
    demoMeals: List<DemoFood>?,
    state: LazyListState,
    entries: List<Entry> = emptyList(),
    prefs: Preferences = Preferences(),
) {
    val colors = nourishColors
    LazyColumn(state = state) {
        item {
            TodayHeader(date, showDemo = demoMeals != null)
        }
        val meals = demoMeals ?: entries.map { DemoFood(it.label.name, it.meal, listOf(it).nutrition()) }
        val total = if (demoMeals == null) entries.nutrition() else DemoNutrition.total(demoMeals)
        val goal = if (demoMeals == null) Nutrition(
            prefs.target("208",date)?.toDoubleOrNull()?.toInt() ?: 0,
            prefs.target("203",date)?.toDoubleOrNull()?.toInt() ?: 0,
            prefs.target("205",date)?.toDoubleOrNull()?.toInt() ?: 0,
            prefs.target("204",date)?.toDoubleOrNull()?.toInt() ?: 0,
        ) else DemoNutrition.goal
        if ("208" !in prefs.hidden) item { HeroMetric(total.calories, goal.calories) }
        if (listOf("203","204","205").any { it !in prefs.hidden }) item { MacroHighlights(total, goal, prefs.hidden) }
        if (demoMeals == null) item { Column(Modifier.padding(horizontal = 20.dp)) { NutrientSummary(entries,date,prefs) } }
        if (demoMeals == null && entries.isEmpty()) {
            item {
                EmptyState(
                    title = "Your day is open",
                    message = "Tap the pink + to log your first food today.",
                    glyph = Glyph.Bowl,
                    accent = colors.butter,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Heading("Today’s meals", Modifier.weight(1f))
                    Text(
                        if (meals.size == 1) "1 food" else "${meals.size} foods",
                        style = NourishType.caption,
                        color = colors.muted,
                    )
                }
            }
            items(meals) { meal ->
                MealRow(meal, Modifier.padding(horizontal = 20.dp))
            }
            if (demoMeals != null) item {
                NutritionSparkline(
                    date,
                    DemoNutrition.week(total.calories),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun TodayHeader(date: LocalDate, showDemo: Boolean) {
    val colors = nourishColors
    val locale = LocalLocale.current.platformLocale
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(25.dp).clip(NourishShape.pill).background(colors.mint),
                    contentAlignment = Alignment.Center,
                ) {
                    NourishIcon(Glyph.Brand, colors.ink, Modifier.size(16.dp))
                }
                Text("nourish", style = NourishType.heading, color = colors.text)
            }
            if (showDemo) {
                Text(
                    "DEMO DATA",
                    Modifier.clip(NourishShape.pill).background(colors.lilac).padding(horizontal = 10.dp, vertical = 6.dp),
                    color = colors.ink,
                    style = NourishType.eyebrow,
                )
            }
        }
        Column {
            Heading("Today", style = NourishType.screen)
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)),
                style = NourishType.body,
                color = colors.muted,
            )
        }
    }
}

@Composable
internal fun HeroMetric(calories: Int, goal: Int, modifier: Modifier = Modifier) {
    val colors = nourishColors
    val fraction by animateFloatAsState(
        targetValue = if (goal > 0) (calories.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        animationSpec = NourishMotion.settle,
        label = "calorie goal progress",
    )
    val difference = goal - calories
    val remaining = if (difference >= 0) "${difference.formatted()} kcal left" else "${(-difference).formatted()} kcal over"
    BoxWithConstraints(modifier.fillMaxWidth().background(colors.butter)) {
        val showArtwork = maxWidth >= 345.dp && LocalDensity.current.fontScale <= 1.2f
        if (showArtwork) {
            CitrusSticker(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 15.dp).size(126.dp))
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 19.dp, bottom = 18.dp)) {
            Text("DAILY ENERGY", color = colors.ink, style = NourishType.eyebrow)
            Text(
                calories.formatted(),
                Modifier.fillMaxWidth().padding(end = if (showArtwork) 101.dp else 0.dp),
                color = colors.ink,
                style = NourishType.hero,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 92.sp),
            )
            Text("kcal logged", color = colors.ink, style = NourishType.strong)
            if (goal > 0) Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 9.dp)
                    .height(6.dp)
                    .clip(NourishShape.pill)
                    .background(colors.ink.copy(alpha = 0.18f)),
            ) {
                Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(NourishShape.pill).background(colors.ink))
            }
            if (goal > 0) Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Goal · ${goal.formatted()} kcal", Modifier.weight(1f), color = colors.ink, style = NourishType.caption)
                Text(
                    remaining,
                    Modifier.clip(NourishShape.pill).background(colors.ink).padding(horizontal = 10.dp, vertical = 4.dp),
                    color = colors.cream,
                    style = NourishType.caption,
                )
            }
        }
    }
}

@Composable
private fun MacroHighlights(total: Nutrition, goal: Nutrition, hidden: Set<String>) {
    val colors = nourishColors
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, top = 19.dp, end = 20.dp)) {
        Heading("Macro mix")
        Text("Grams eaten", color = colors.muted, style = NourishType.caption)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            val stack = maxWidth < 290.dp || LocalDensity.current.fontScale > 1.3f
            if (stack) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("203" !in hidden) PastelMetricCard("Protein", total.protein, goal.protein, colors.pink, Sticker.Petal, Modifier.fillMaxWidth())
                    if ("205" !in hidden) PastelMetricCard("Carbs", total.carbs, goal.carbs, colors.blue, Sticker.Burst, Modifier.fillMaxWidth())
                    if ("204" !in hidden) PastelMetricCard("Fat", total.fat, goal.fat, colors.mint, Sticker.Heart, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("203" !in hidden) PastelMetricCard("Protein", total.protein, goal.protein, colors.pink, Sticker.Petal, Modifier.weight(1f))
                    if ("205" !in hidden) PastelMetricCard("Carbs", total.carbs, goal.carbs, colors.blue, Sticker.Burst, Modifier.weight(1f))
                    if ("204" !in hidden) PastelMetricCard("Fat", total.fat, goal.fat, colors.mint, Sticker.Heart, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun PastelMetricCard(
    label: String,
    grams: Int,
    goal: Int,
    accent: Color,
    sticker: Sticker,
    modifier: Modifier = Modifier,
) {
    val ink = nourishColors.ink
    val fraction by animateFloatAsState(
        targetValue = if (goal > 0) (grams.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        animationSpec = NourishMotion.settle,
        label = "macro goal progress",
    )
    Box(modifier.clip(NourishShape.panel).background(accent).semantics(mergeDescendants = true) {}) {
        StickerShape(sticker, ink.copy(alpha = 0.105f), Modifier.align(Alignment.TopEnd).size(58.dp))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = ink, style = NourishType.caption)
            Text(
                grams.formatted(),
                Modifier.fillMaxWidth(),
                color = ink,
                style = NourishType.metric,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 34.sp),
            )
            Text(if (goal > 0) "/ ${goal.formatted()} g" else "grams", color = ink, style = NourishType.caption)
            if (goal > 0) Box(
                Modifier.fillMaxWidth().padding(top = 7.dp).height(4.dp)
                    .clip(NourishShape.pill).background(ink.copy(alpha = 0.18f)),
            ) {
                Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(NourishShape.pill).background(ink))
            }
        }
    }
}

@Composable
private fun MealRow(meal: DemoFood, modifier: Modifier = Modifier) {
    val colors = nourishColors
    val accent = when (meal.meal) {
        "Breakfast" -> colors.lilac
        "Lunch" -> colors.mint
        else -> colors.peach
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(39.dp).clip(NourishShape.small).background(accent), contentAlignment = Alignment.Center) {
                NourishIcon(if (meal.meal == "Breakfast") Glyph.Sun else Glyph.Bowl, colors.ink, Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(vertical = 7.dp)) {
                Text(meal.name, style = NourishType.strong, color = colors.text)
                Text(meal.meal, style = NourishType.caption, color = colors.muted)
            }
            Text("${meal.nutrition.calories.formatted()} kcal", style = NourishType.caption, color = colors.text)
        }
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
internal fun NutritionSparkline(date: LocalDate, calories: List<Int>, modifier: Modifier = Modifier, sample: Boolean = true) {
    val colors = nourishColors
    val maximum = maxOf(calories.max(), 1)
    val days = calories.indices.map { date.minusDays((calories.lastIndex - it).toLong()) }
    val locale = LocalLocale.current.platformLocale
    val kcalFormat = NumberFormat.getIntegerInstance(locale)
    val weekdayFormat = DateTimeFormatter.ofPattern("EEEE", locale)
    val narrowDayFormat = DateTimeFormatter.ofPattern("EEEEE", locale)
    val readings = days.zip(calories).joinToString { (day, value) ->
        "${day.format(weekdayFormat)}, ${kcalFormat.format(value)} kcal"
    }
    Column(
        modifier.fillMaxWidth().clip(NourishShape.panel).background(colors.lilac).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("7-day energy", Modifier.semantics { heading() }, style = NourishType.heading, color = colors.ink)
        Text(if (sample) "Daily calories · sample week" else "Logged calories · past seven days", style = NourishType.caption, color = colors.ink)
        Row(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = readings },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            calories.forEachIndexed { index, value ->
                val latest = index == calories.lastIndex
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier.width(18.dp).height((52f * value / maximum).dp).clip(NourishShape.pill)
                            .background(if (latest) colors.ink else colors.ink.copy(alpha = 0.46f)),
                    )
                    Text(
                        days[index].format(narrowDayFormat),
                        style = NourishType.caption,
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

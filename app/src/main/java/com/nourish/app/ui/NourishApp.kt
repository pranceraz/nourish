package com.nourish.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.nourish.app.diary.*
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import kotlinx.coroutines.CancellationException
import com.nourish.app.ui.today.NutritionSparkline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nourish.app.BuildConfig
import com.nourish.app.model.DemoNutrition
import com.nourish.app.ui.theme.NourishMotion
import com.nourish.app.ui.theme.NourishShape
import com.nourish.app.ui.theme.NourishTheme
import com.nourish.app.ui.theme.NourishType
import com.nourish.app.ui.theme.nourishColors
import com.nourish.app.ui.today.TodayScreen
import java.time.LocalDate
import kotlinx.coroutines.launch

internal enum class Destination(val label: String, val glyph: Glyph) {
    Today("Today", Glyph.Today),
    Diary("Diary", Glyph.Diary),
    Insights("Insights", Glyph.Insights),
    Settings("Settings", Glyph.Settings),
}

@Composable
internal fun NourishApp(
    date: LocalDate,
    initiallyShowDemo: Boolean = true,
    dao: DiaryDao? = null,
    store: DiaryPreferences? = null,
    initialDestination: Destination = Destination.Today,
) {
    NourishTheme {
        val colors = nourishColors
        var destination by rememberSaveable { mutableStateOf(initialDestination) }
        val preferences by remember(store) { store?.values ?: flowOf(Preferences(initiallyShowDemo)) }
            .collectAsState(initial = if (store == null) Preferences(initiallyShowDemo) else null)
        val prefs = preferences ?: return@NourishTheme
        var quickAddOpen by rememberSaveable { mutableStateOf(false) }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val diaryDate = selected ?: date.toString()
        var editorDate by rememberSaveable { mutableStateOf(date.toString()) }
        var editId by rememberSaveable { mutableStateOf<String?>(null) }
        val weekStart = date.minusDays(6).toString()
        val week by remember(dao, date) { dao?.entries(weekStart, date.toString()) ?: flowOf(emptyList<Entry>()) }
            .collectAsState(emptyList())
        val day by androidx.compose.runtime.key(diaryDate) {
            remember(dao, diaryDate) { dao?.entries(diaryDate, diaryDate) ?: flowOf(emptyList<Entry>()) }
                .collectAsState(emptyList())
        }
        val foods by remember(dao) { dao?.foods() ?: flowOf(emptyList<Food>()) }.collectAsState(emptyList())
        fun add() {
            editId = null
            editorDate = if (destination == Destination.Diary) diaryDate else date.toString()
            quickAddOpen = true
        }
        val todayScroll = rememberLazyListState()
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        fun write(action: suspend () -> Unit) {
            scope.launch {
                try { action() }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { snackbars.showSnackbar("Couldn’t save that change. Please try again.") }
            }
        }
        BackHandler(enabled = destination != Destination.Today && !quickAddOpen) {
            destination = Destination.Today
        }

        NourishScaffold(
            destination = destination,
            onNavigate = { destination = it },
            onQuickAdd = ::add,
            snackbars = snackbars,
        ) {
            Crossfade(destination, animationSpec = NourishMotion.selection, label = "tab change") { screen ->
                when (screen) {
                    Destination.Today -> TodayScreen(date, if (prefs.demo) DemoNutrition.starterMeals else null,
                        todayScroll, week.filter { it.date == date.toString() }, prefs)
                    Destination.Diary -> DiaryScreen(LocalDate.parse(diaryDate), day,
                        onDate = { selected = it.toString() }, onEdit = {
                            editId = it.id; editorDate = diaryDate; quickAddOpen = true
                        }, onDuplicate = { entry ->
                            val id = UUID.randomUUID().toString()
                            write { requireNotNull(dao).duplicate(entry.id, id) }
                        }, onDelete = { entry -> write { requireNotNull(dao).delete(entry.id) } }, onAdd = ::add, prefs = prefs)
                    Destination.Insights -> PageContent("Insights") {
                        if (week.isEmpty()) EmptyState("A view of your week",
                            "Log food this week to see your daily totals.", Glyph.Insights, colors.blue)
                        else if ("208" !in prefs.hidden) NutritionSparkline(date, (6 downTo 0).map { offset ->
                            week.filter { it.date == date.minusDays(offset.toLong()).toString() }.nutrition().calories
                        }, sample = false)
                        NutrientSummary(week,date,prefs,weekly = true)
                    }
                    Destination.Settings -> SettingsScreen(prefs.demo,
                        onDemoChanged = { enabled -> write { requireNotNull(store).demo(enabled) } })
                }
            }
        }

        if (quickAddOpen && dao != null) {
            val original = day.find { it.id == editId }
            if (editId == null || original != null) EntryEditor(editorDate, original, foods,
                onClose = { quickAddOpen = false },
                onFavorite = { food -> write { dao.favorite(food.id, !food.favorite) } },
                onSaveMeal = { entries, photo ->
                    dao.saveMeal(entries,photo)
                    selected = editorDate
                    destination = Destination.Diary
                },
                loadPhoto = dao::photo,
                onSave = { entry, code, changed, jpeg ->
                    dao.saveWithInputs(entry, editId != null, code, changed, jpeg)
                    selected = editorDate
                    destination = Destination.Diary
                })
        }
    }
}

@Composable
private fun NourishScaffold(
    destination: Destination,
    onNavigate: (Destination) -> Unit,
    onQuickAdd: () -> Unit,
    snackbars: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    val colors = nourishColors
    Box(
        Modifier.fillMaxSize().background(colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                content()
                SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter).padding(12.dp)) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = NourishShape.small,
                        containerColor = colors.ink,
                        contentColor = colors.cream,
                    )
                }
            }
            NourishBottomBar(
                destination,
                onNavigate,
                onQuickAdd,
                Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
internal fun NourishBottomBar(
    current: Destination,
    onNavigate: (Destination) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nourishColors
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val raisedButton = maxWidth < 315.dp || LocalDensity.current.fontScale > 1.4f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (raisedButton) {
                QuickAddButton(onQuickAdd)
                Spacer(Modifier.height(4.dp))
            }
            Row(
                Modifier.fillMaxWidth().clip(NourishShape.dock).background(colors.ink)
                    .selectableGroup().padding(horizontal = 5.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Destination.entries.forEachIndexed { index, destination ->
                    if (index == 2 && !raisedButton) {
                        QuickAddButton(onQuickAdd, Modifier.padding(horizontal = 3.dp))
                    }
                    DockTab(destination, destination == current, { onNavigate(destination) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DockTab(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nourishColors
    Column(
        modifier.clip(NourishShape.small).selectable(selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 54.dp).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.width(37.dp).height(25.dp).clip(NourishShape.pill)
                .then(if (selected) Modifier.background(colors.butter) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            NourishIcon(destination.glyph, if (selected) colors.ink else colors.dockMuted, Modifier.size(17.dp))
        }
        Text(
            destination.label,
            color = if (selected) colors.cream else colors.dockMuted,
            style = NourishType.tab,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = nourishColors
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Box(
        modifier.size(53.dp).semantics { contentDescription = "Quick add meal" }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(NourishShape.pill).background(colors.pink)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NourishIcon(Glyph.Add, colors.ink, Modifier.size(27.dp))
    }
}

@Composable
private fun PageContent(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Heading(title, style = NourishType.screen)
        content()
    }
}

@Composable
private fun SettingsScreen(showDemo: Boolean, onDemoChanged: (Boolean) -> Unit) {
    val colors = nourishColors
    PageContent("Settings") {
        NutrientSettings()
        ConnectionSettings()
        Column(
            Modifier.fillMaxWidth().clip(NourishShape.panel).background(colors.panel).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Heading("Demo data", style = NourishType.heading)
                    Text("Show sample nutrition and meals on Today.", color = colors.muted, style = NourishType.body)
                }
                Switch(
                    checked = showDemo,
                    onCheckedChange = onDemoChanged,
                    modifier = Modifier.semantics { contentDescription = "Show demo data" },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.ink,
                        checkedTrackColor = colors.pink,
                        uncheckedThumbColor = colors.muted,
                        uncheckedTrackColor = colors.panel,
                        uncheckedBorderColor = colors.muted,
                    ),
                )
            }
            HorizontalDivider(color = colors.divider)
            Text(
                "Demo affects Today only. The pink + always saves to your own diary.",
                color = colors.muted,
                style = NourishType.caption,
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(NourishShape.panel).background(colors.panel).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Heading("About", style = NourishType.heading)
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = colors.text, style = NourishType.body)
            Text("Build ${BuildConfig.BUILD_ID.take(12)}", color = colors.muted, style = NourishType.body)
        }
    }
}

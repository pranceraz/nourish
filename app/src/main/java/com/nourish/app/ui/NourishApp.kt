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
import com.nourish.app.ui.add.QuickAddSheet
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
    onDemoPreferenceChanged: (Boolean) -> Unit = {},
    initialDestination: Destination = Destination.Today,
) {
    NourishTheme {
        val colors = nourishColors
        var destination by rememberSaveable { mutableStateOf(initialDestination) }
        var showDemo by rememberSaveable { mutableStateOf(initiallyShowDemo) }
        var quickAddOpen by rememberSaveable { mutableStateOf(false) }
        var additions by rememberSaveable { mutableStateOf(intArrayOf()) }
        val demoMeals = remember(showDemo, additions) {
            if (showDemo) DemoNutrition.meals(additions.toList()) else null
        }
        val todayScroll = rememberLazyListState()
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        BackHandler(enabled = destination != Destination.Today && !quickAddOpen) {
            destination = Destination.Today
        }

        NourishScaffold(
            destination = destination,
            onNavigate = { destination = it },
            onQuickAdd = { quickAddOpen = true },
            snackbars = snackbars,
        ) {
            Crossfade(destination, animationSpec = NourishMotion.selection, label = "tab change") { screen ->
                when (screen) {
                    Destination.Today -> TodayScreen(date, demoMeals, todayScroll)
                    Destination.Diary -> PageContent("Diary") {
                        EmptyState(
                            "Your meal log",
                            "The offline diary will let you log, edit, and revisit your own meals.",
                            Glyph.Diary,
                            colors.peach,
                        )
                    }
                    Destination.Insights -> PageContent("Insights") {
                        EmptyState(
                            "A view of your week",
                            "Your trends will show up here once the diary has meals to summarize.",
                            Glyph.Insights,
                            colors.blue,
                        )
                    }
                    Destination.Settings -> SettingsScreen(showDemo) { enabled ->
                        showDemo = enabled
                        onDemoPreferenceChanged(enabled)
                    }
                }
            }
        }

        if (quickAddOpen) {
            QuickAddSheet(
                showDemo = showDemo,
                onDismiss = { quickAddOpen = false },
                onAddSample = { index ->
                    additions += index
                    destination = Destination.Today
                    scope.launch {
                        todayScroll.requestScrollToItem(0)
                        snackbars.currentSnackbarData?.dismiss()
                        snackbars.showSnackbar("Added to demo · ${DemoNutrition.options[index].name}")
                    }
                },
                onOpenSettings = { destination = Destination.Settings },
            )
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
                "Sample additions stay in the current app session. This setting is saved on this device.",
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

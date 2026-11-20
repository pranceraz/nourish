package com.nourish.app.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nourish.app.model.DemoNutrition
import com.nourish.app.ui.Glyph
import com.nourish.app.ui.NourishIcon
import com.nourish.app.ui.PrimaryPillButton
import com.nourish.app.ui.formatted
import com.nourish.app.ui.theme.NourishShape
import com.nourish.app.ui.theme.NourishType
import com.nourish.app.ui.theme.nourishColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickAddSheet(
    showDemo: Boolean,
    onDismiss: () -> Unit,
    onAddSample: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = nourishColors
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var browsing by rememberSaveable { mutableStateOf(false) }
    var selection by rememberSaveable { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }

    fun close(onCommit: () -> Unit = {}) {
        if (closing) return
        closing = true
        onCommit()
        scope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = NourishShape.sheet,
        containerColor = colors.ink,
        contentColor = colors.cream,
        tonalElevation = 0.dp,
    ) {
        QuickAddPanel(
            showDemo = showDemo,
            browsing = browsing,
            selection = selection,
            onBrowse = { browsing = true },
            onSelection = { selection = it },
            onBack = { browsing = false },
            onClose = { close() },
            onAdd = { close { onAddSample(selection) } },
            onOpenSettings = { close(onOpenSettings) },
        )
    }
}

@Composable
internal fun QuickAddPanel(
    showDemo: Boolean,
    browsing: Boolean,
    selection: Int,
    onBrowse: () -> Unit,
    onSelection: (Int) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = nourishColors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        Modifier.fillMaxWidth().background(colors.ink).verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (browsing) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back to quick add" }) {
                    NourishIcon(if (rtl) Glyph.Arrow else Glyph.Back, colors.cream)
                }
            }
            Text(
                if (browsing) "Sample foods" else "Quick add",
                Modifier.weight(1f).semantics { heading() },
                style = NourishType.section,
                color = colors.cream,
            )
            IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = "Close quick add" }) {
                Box(
                    Modifier.size(36.dp).clip(NourishShape.pill).background(colors.dockPanel),
                    contentAlignment = Alignment.Center,
                ) {
                    NourishIcon(Glyph.Close, colors.cream, Modifier.size(19.dp))
                }
            }
        }
        if (browsing) {
            Text("Sample entries only affect this session’s demo totals.", style = NourishType.body, color = colors.dockMuted)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoNutrition.options.forEachIndexed { index, food ->
                    val selected = index == selection
                    Row(
                        Modifier.fillMaxWidth().clip(NourishShape.small).background(colors.dockPanel)
                            .then(if (selected) Modifier.border(2.dp, colors.pink, NourishShape.small) else Modifier)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelection(index) })
                            .heightIn(min = 54.dp)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name, style = NourishType.strong, color = colors.cream)
                            Text("${food.nutrition.calories.formatted()} kcal", style = NourishType.caption, color = colors.dockMuted)
                        }
                        if (selected) NourishIcon(Glyph.Check, colors.pink)
                    }
                }
            }
            PrimaryPillButton("Add to demo", onAdd, Modifier.fillMaxWidth(), Glyph.Add, colors.pink, colors.ink)
        } else {
            if (showDemo) {
                Text("Try adding a sample food to Today.", style = NourishType.body, color = colors.dockMuted)
                PrimaryPillButton(
                    "Browse sample foods", onBrowse, Modifier.fillMaxWidth(), if (rtl) Glyph.Back else Glyph.Arrow,
                    colors.pink, colors.ink,
                )
            } else {
                Text("Manual logging arrives with the offline diary. To try Quick Add now, turn on sample nutrition.", style = NourishType.body, color = colors.dockMuted)
                PrimaryPillButton(
                    "Demo settings", onOpenSettings, Modifier.fillMaxWidth(), if (rtl) Glyph.Back else Glyph.Arrow,
                    colors.pink, colors.ink,
                )
            }
            HorizontalDivider(color = colors.dockPanel)
            Text("IN DEVELOPMENT", style = NourishType.eyebrow, color = colors.dockMuted)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FutureMethod("Describe food", Glyph.Text, colors.lilac)
                FutureMethod("Take photo", Glyph.Camera, colors.butter)
                FutureMethod("Choose photo", Glyph.Photo, colors.blue)
                FutureMethod("Search food", Glyph.Search, colors.mint)
                FutureMethod("Scan barcode", Glyph.Barcode, colors.peach)
            }
        }
    }
}

@Composable
private fun FutureMethod(label: String, glyph: Glyph, accent: Color) {
    val colors = nourishColors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(31.dp).clip(NourishShape.pill).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            NourishIcon(glyph, colors.ink, Modifier.size(17.dp))
        }
        Text(label, style = NourishType.body, color = colors.cream)
    }
}

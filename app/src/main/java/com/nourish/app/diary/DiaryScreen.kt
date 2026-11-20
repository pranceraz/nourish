package com.nourish.app.diary

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.*
import com.nourish.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun DiaryScreen(
    date: LocalDate, entries: List<Entry>, onDate: (LocalDate) -> Unit, onEdit: (Entry) -> Unit,
    onDuplicate: (Entry) -> Unit, onDelete: (Entry) -> Unit, onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val colors = nourishColors
    var remove by rememberSaveable(date) { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Heading("Diary", style = NourishType.screen)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onDate(date.minusDays(1)) }) { Text("Earlier") }
                TextButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d -> onDate(LocalDate.of(y,m+1,d)) },
                        date.year,date.monthValue-1,date.dayOfMonth).show()
                }) { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
                TextButton(onClick = { onDate(date.plusDays(1)) }) { Text("Later") }
            }
            val total = entries.nutrition()
            Column(Modifier.fillMaxWidth().clip(NourishShape.panel).background(colors.butter).padding(18.dp)) {
                Text("DAILY TOTAL", style = NourishType.eyebrow, color = colors.ink)
                Text("${total.calories} kcal", style = NourishType.screen, color = colors.ink)
                Text("Protein ${total.protein} g  ·  Carbs ${total.carbs} g  ·  Fat ${total.fat} g", color = colors.ink)
            }
        }
        if (entries.isEmpty()) item {
            EmptyState("Your meal log", "Add the first food for this day.", Glyph.Bowl, colors.peach)
            PrimaryPillButton("Log food", onAdd)
        }
        mealTypes.forEach { type ->
            val group = entries.filter { it.meal == type }
            if (group.isNotEmpty()) {
                item { Heading("$type · ${group.nutrition().calories} kcal", style = NourishType.heading) }
                items(group, key = { it.id }) { entry ->
                    var menu by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().clip(NourishShape.panel).background(colors.panel).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.label.name, style = NourishType.strong, color = colors.text)
                            val unit = if (entry.label.unit == "Grams") "g" else "servings"
                            Text("${amountText(entry.quantity)} $unit · ${listOf(entry).nutrition().calories} kcal",
                                color = colors.muted, style = NourishType.caption)
                        }
                        Box {
                            TextButton(onClick = { menu = true }) { Text("Actions") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(entry) })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate(entry) })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; remove = entry.id })
                            }
                        }
                    }
                }
            }
        }
    }
    entries.find { it.id == remove }?.let { entry ->
        AlertDialog(onDismissRequest = { remove = null }, title = { Text("Delete ${entry.label.name}?") },
            text = { Text("This removes this portion from your diary. The food stays available for reuse.") },
            confirmButton = { TextButton(onClick = { remove = null; onDelete(entry) }) { Text("Delete entry") } },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("Keep entry") } })
    }
}

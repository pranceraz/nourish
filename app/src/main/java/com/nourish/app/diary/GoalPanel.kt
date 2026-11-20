package com.nourish.app.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nourish.app.model.Nutrition
import com.nourish.app.ui.Heading
import com.nourish.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun GoalPanel(goals: Nutrition, onSave: suspend (List<Int>) -> Unit) {
    val scope = rememberCoroutineScope()
    val labels = listOf("Energy (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)")
    val saved = listOf(goals.calories, goals.protein, goals.carbs, goals.fat)
    var editing by rememberSaveable { mutableStateOf(false) }
    var values by rememberSaveable { mutableStateOf<List<String>>(arrayListOf()) }
    var error by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(NourishShape.panel).background(nourishColors.panel).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Heading("Daily goals", style = NourishType.heading)
        if (editing) {
            Text("Set your own daily targets. Leave a field empty to clear it.", color = nourishColors.text)
            labels.forEachIndexed { i, label -> NumberField(label, values[i]) { value ->
                values = ArrayList(values).apply { set(i,value) }
            } }
            if (error.isNotEmpty()) Text(error, color = nourishColors.text)
            Row {
                Button(enabled = !busy, onClick = {
                    val numbers = values.map { if (it.isBlank()) 0 else it.toIntOrNull() }
                    if (numbers.any { it == null || it !in 0..99999 }) error = "Enter whole numbers up to 99,999."
                    else if (!busy) {
                        busy = true
                        scope.launch {
                            try { onSave(numbers.filterNotNull()); editing = false }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { error = "Couldn’t save your goals. Try again." }
                            finally { busy = false }
                        }
                    }
                }) { Text("Save goals") }
                TextButton(enabled = !busy, onClick = { editing = false }) { Text("Cancel") }
            }
        } else {
            val summary = labels.zip(saved).filter { it.second > 0 }.joinToString(" · ") { (label, value) -> "$label $value" }
            Text(summary.ifEmpty { "Set personal targets to see your progress on Today." }, color = nourishColors.text)
            OutlinedButton(onClick = {
                values = ArrayList(saved.map { if (it == 0) "" else it.toString() })
                error = ""
                editing = true
            }) { Text("Edit goals") }
        }
    }
}

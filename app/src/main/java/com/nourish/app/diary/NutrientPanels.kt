package com.nourish.app.diary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.Heading
import java.time.LocalDate
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
private fun nutrientList(): List<Nutrient> {
    val context = LocalContext.current
    val nutrients by produceState(emptyList<Nutrient>()) {
        value = withContext(Dispatchers.IO) { SurveyCatalog.get(context).nutrients }
    }
    return nutrients
}

@Composable
internal fun FoodDetails(details: String?, update: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val data = remember(details) { JSONObject(details ?: "{}") }
    val nutrients = data.optJSONObject("nutrients") ?: JSONObject()
    if (details != null) {
        Text(listOf(data.optString("source","User-entered nutrition"),data.optString("macroSource"))
            .filter { it.isNotBlank() }.joinToString(" · "))
    }
    OutlinedButton(onClick = { open = !open }) { Text("Ingredients, vitamins & minerals") }
    if (!open) return
    data.optJSONArray("ingredients")?.let { rows ->
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            Text("${row.getString("food")} · ${row.getString("grams")} g · FDC ${row.getString("id")}")
            if (row.optString("assumption").isNotBlank()) Text(row.getString("assumption"))
        }
    }
    Text("Values use the same label basis as the macros. Leave unknown values blank.")
    nutrientList().drop(4).forEach { n ->
        // Keep partial decimal input editable; normalize before committing the field.
        var value by rememberSaveable(n.number) { mutableStateOf(nutrients.optString(n.number)) }
        NumberField("${n.name} (${n.unit})",value) { text ->
            if (text.isBlank() || text.matches(Regex("""\d{0,5}([.,]\d{0,3})?"""))) {
                value = text
                val amount = text.replace(',','.').toBigDecimalOrNull()
                if (amount == null) nutrients.remove(n.number) else nutrients.put(n.number,amount.toPlainString())
                data.optJSONObject("partial")?.remove(n.number)
                update(data.put("source","User-edited nutrient profile").put("nutrients",nutrients).toString())
            }
        }
    }
}

@Composable
internal fun NutrientSummary(entries: List<Entry>, date: LocalDate, prefs: Preferences, weekly: Boolean = false) {
    if (entries.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = !open }) { Text(if (weekly) "Logged nutrients this week" else "Vitamins & minerals") }
    if (!open) return
    val list = nutrientList().filter { (weekly || it.group in setOf("Vitamins","Minerals")) && it.number !in prefs.hidden }
    list.forEach { n ->
        val reported = entries.mapNotNull { it.amount(n.number) }
        val target = if (weekly) null else prefs.target(n.number,date)
        val total = if (reported.isEmpty()) "Unknown" else "${reported.reduce { a,b -> a+b }.display()} ${n.unit}"
        val incompleteDish = entries.any { e -> e.label.details?.let { JSONObject(it).optJSONObject("partial")?.optBoolean(n.number) } == true }
        val coverage = if (reported.isNotEmpty() && (reported.size < entries.size || incompleteDish)) " · partial" else ""
        Text("${n.name} · $total$coverage" + (target?.let { " · goal $it ${n.unit}" } ?: ""))
    }
}

@Composable
internal fun NutrientSettings() {
    val context = LocalContext.current
    val store = remember { DiaryPreferences(context) }
    val connection = remember { DraftConnection(context) }
    val prefs by store.values.collectAsState(Preferences())
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf("303") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun run(action: suspend () -> Unit) {
        scope.launch {
            try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { reason = (e as? ServiceError)?.message ?: "Couldn’t complete that change. Try again." }
        }
    }
    OutlinedButton(onClick = { open = !open }) { Text("Nutrients & daily goals") }
    if (!open) return
    val list = nutrientList()
    val n = list.find { it.number == selected } ?: return
    Heading("Personal targets")
    val date = LocalDate.now()
    if (!busy) Choices("Nutrient",n.name,list.map { it.name }) { name ->
        selected = list.first { it.name == name }.number; amount = ""; reason = ""; note = ""
    }
    Text("Current · " + (prefs.target(n.number,date)?.let { "$it ${n.unit} / day" } ?: "No target"))
    NumberField("Proposed daily target (${n.unit})",amount) { amount = it }
    if (connection.origin.isNotEmpty()) {
        OutlinedTextField(note,{ note = it.take(600) },label = { Text("What would you like to track?") })
        TextButton(enabled = !busy && note.isNotBlank(),onClick = {
            busy = true
            run {
                try {
                    val result = connection.request("/v1/goals","POST",JSONObject().put("nutrient",n.number).put("context",note))
                    amount = result.getString("amount").takeUnless { it.toBigDecimal().signum() == 0 }.orEmpty()
                    reason = result.getString("rationale")
                } finally { busy = false }
            }
        }) { Text("Ask GPT for a suggestion") }
    }
    if (reason.isNotBlank()) Text(reason)
    Row {
        TextButton(enabled = !busy && (parseAmount(amount) ?: 0) > 0,onClick = { run {
            store.target(n.number,date,amount.replace(',','.')); reason = "Target applied from $date."; amount = ""
        } }) { Text("Apply target") }
        TextButton(enabled = !busy,onClick = { run { store.target(n.number,date,""); amount = ""; reason = "Target cleared." } }) { Text("Clear target") }
    }
    Heading("Visible nutrients")
    list.groupBy { it.group }.forEach { (group,items) ->
        Text(group)
        items.forEach { item ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, Modifier.weight(1f))
                Switch(checked = item.number !in prefs.hidden, onCheckedChange = { show -> run { store.visible(item.number,show) } },
                    modifier = Modifier.semantics { contentDescription = "Show ${item.name}" })
            }
        }
    }
}

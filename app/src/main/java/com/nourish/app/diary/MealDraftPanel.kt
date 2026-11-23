package com.nourish.app.diary

import android.util.Base64
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.Heading
import com.nourish.app.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun MealDraftPanel(
    date: String, intentId: String, meal: String, onMeal: (String) -> Unit,
    photo: suspend () -> ByteArray?, hasPhoto: Boolean,
    onSave: suspend (List<Entry>, ByteArray?) -> Unit, onClose: () -> Unit,
) {
    val context = LocalContext.current
    val catalog by produceState<SurveyCatalog?>(null) {
        value = withContext(Dispatchers.IO) { SurveyCatalog.get(context) }
    }
    val service = remember { DraftConnection(context) }
    val scope = rememberCoroutineScope()
    var description by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var rowsText by rememberSaveable { mutableStateOf("[]") }
    val rows = remember(rowsText) { JSONArray(rowsText) }
    var replace by rememberSaveable { mutableIntStateOf(-1) }
    var asDish by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    fun change(action: (JSONArray) -> Unit) {
        val edited = JSONArray(rowsText)
        action(edited)
        rowsText = edited.toString()
    }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = ""
        scope.launch {
            try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { message = (e as? ServiceError)?.message ?: "Couldn’t finish. Your draft is here to review or retry." }
            finally { busy = false }
        }
    }

    var showAi by rememberSaveable { mutableStateOf(false) }
    if (service.origin.isNotEmpty()) TextButton(onClick = { showAi = !showAi }) {
        Text(if (showAi) "Hide AI options" else "Describe a meal with AI")
    }
    if (showAi && service.origin.isNotEmpty()) {
        OutlinedTextField(description,{ description = it.take(1500) }, Modifier.fillMaxWidth(),
            label = { Text("Describe the dish and portion") })
        Text("Sends the description${if (hasPhoto) " and attached photo" else ""} to your Nourish server and OpenAI.")
        Button(enabled = !busy && (description.isNotBlank() || hasPhoto), onClick = { run {
            val request = JSONObject().put("description",description)
            photo()?.let { request.put("jpeg",Base64.encodeToString(it,Base64.NO_WRAP)) }
            val result = service.request("/v1/drafts","POST",request)
            val incoming = result.getJSONArray("ingredients")
            val nextRows = JSONArray()
            for (i in 0 until incoming.length()) {
                val item = incoming.getJSONObject(i)
                val candidates = item.getJSONArray("candidates")
                nextRows.put(JSONObject().put("id",candidates.optString(0)).put("grams",item.get("grams").toString())
                    .put("assumption",item.getString("assumption")).put("food",item.getString("food")))
            }
            question = result.getString("question")
            if (nextRows.length() > 0) {
                rowsText = nextRows.toString(); replace = -1; name = result.getString("name").take(100)
            } else if (question.isBlank()) message = "No ingredients identified. Search the USDA catalog below."
        } }) { Text(if (busy) "Working…" else "Generate ingredient draft") }
    }
    if (question.isNotBlank()) Text("Clarify before saving · $question")
    val options = remember(catalog,search) { catalog?.search(search).orEmpty() }
    OutlinedTextField(search,{ search = it.take(120) },Modifier.fillMaxWidth(),
        label = { Text(if (replace < 0) "Find a USDA food or dish" else "Find replacement food") }, singleLine = true)
    if (search.isNotBlank() && options.isEmpty() && catalog != null) Text("No match. Try a simpler ingredient name.")
    options.forEach { food ->
        TextButton(enabled = !busy, onClick = {
            change { items ->
                if (replace in 0 until items.length()) {
                    items.getJSONObject(replace).put("id",food.id).put("food",food.name)
                } else items.put(JSONObject().put("id",food.id).put("grams","100").put("food",food.name)
                    .put("assumption","Portion entered by user"))
            }
            if (name.isBlank()) name = food.name.take(100)
            replace = -1; search = ""
        }) { Text(food.name) }
    }
    val portions = mutableListOf<Portion>()
    for (i in 0 until rows.length()) {
        val row = rows.getJSONObject(i)
        val food = catalog?.byId?.get(row.getString("id"))
        val grams = parseAmount(row.getString("grams"))?.takeIf { it > 0 }
        if (food != null && grams != null) portions.add(Portion(food, java.math.BigDecimal.valueOf(grams,3),row.optString("assumption")))
        Column(Modifier.fillMaxWidth().clip(NourishShape.panel).background(nourishColors.panel)
            .border(1.dp, nourishColors.text.copy(alpha = 0.16f), NourishShape.panel).padding(14.dp)) {
            Text(food?.name ?: row.optString("food"),style = NourishType.strong,color = nourishColors.text)
            Text(food?.let { "USDA FDC ${it.id}" } ?: "Choose a matching USDA food.",color = nourishColors.muted)
            Text(row.optString("assumption"), color = nourishColors.muted)
            GramsField(row.getString("grams"),row.optString("food")) { v -> change { it.getJSONObject(i).put("grams",v) } }
            Row {
                TextButton(onClick = { replace = i; search = row.optString("food") }) { Text("Change food") }
                TextButton(onClick = { change { it.remove(i) }; replace = -1 }) { Text("Remove") }
            }
        }
    }
    if (rows.length() > 0) {
        val ready = portions.size == rows.length()
        if (ready) {
            Column(Modifier.fillMaxWidth().clip(NourishShape.panel).background(nourishColors.butter).padding(14.dp)) {
                Text("COMBINED TOTAL", style = NourishType.eyebrow, color = nourishColors.ink)
                Text("${nutrientAmount(portions,"208")?.display()} kcal", style = NourishType.heading, color = nourishColors.ink)
                Text(listOf("203" to "Protein","205" to "Carbs","204" to "Fat").joinToString(" · ") { (id,title) ->
                    "$title ${nutrientAmount(portions,id)?.display()} g"
                }, color = nourishColors.ink)
            }
        }
        Choices("Meal",meal,mealTypes,onMeal)
        Text("Save as", style = NourishType.strong)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !asDish, onClick = { asDish = false }, label = { Text("Ingredient breakdown") })
            FilterChip(selected = asDish, onClick = { asDish = true }, label = { Text("Complete dish") })
        }
        if (asDish) OutlinedTextField(name,{ name = it.take(100) }, label = { Text("Dish name") })
        Button(enabled = ready && !busy && (!asDish || name.isNotBlank()), onClick = { run {
            val now = System.currentTimeMillis()
            val groups = if (asDish) listOf(portions.toList()) else portions.map { listOf(it) }
            val entries = groups.mapIndexed { index, group ->
                val id = UUID.nameUUIDFromBytes("$intentId/$index".toByteArray()).toString()
                val label = portionLabel(if (asDish) name else group.first().food.name,group,asDish)
                val catalogId = if (asDish) id else "usda-${group.first().food.id}"
                Entry(id,date,meal,catalogId,label,if (asDish) 1000 else group.first().grams.movePointRight(3).longValueExact(),now+index)
            }
            onSave(entries,photo())
            onClose()
        } }) { Text("Save reviewed meal") }
    }
    if (message.isNotEmpty()) Text(message)
}

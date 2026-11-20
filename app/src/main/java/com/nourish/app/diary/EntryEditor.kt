package com.nourish.app.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nourish.app.ui.Heading
import com.nourish.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryEditor(
    date: String, original: Entry?, foods: List<Food>, onClose: () -> Unit,
    onFavorite: (Food) -> Unit, onSave: suspend (Entry, String?, Boolean, ByteArray?) -> Unit,
    loadPhoto: suspend (String) -> ByteArray? = { null },
) {
    val scope = rememberCoroutineScope()
    val entryId = rememberSaveable { original?.id ?: UUID.randomUUID().toString() }
    val newFoodId = rememberSaveable { UUID.randomUUID().toString() }
    var foodId by rememberSaveable { mutableStateOf(original?.foodId ?: newFoodId) }
    var picking by rememberSaveable { mutableStateOf(original == null) }
    var name by rememberSaveable { mutableStateOf(original?.label?.name.orEmpty()) }
    var meal by rememberSaveable { mutableStateOf(original?.meal ?: "Breakfast") }
    var unit by rememberSaveable { mutableStateOf(original?.label?.unit ?: "Serving") }
    var prep by rememberSaveable { mutableStateOf(original?.label?.preparation ?: "Unspecified") }
    var qty by rememberSaveable { mutableStateOf(original?.quantity?.let(::amountText) ?: "1") }
    fun numbers(label: Label?) = label?.let { listOf(it.kcal,it.protein,it.carbs,it.fat).map(::amountText) } ?: List(4) { "" }
    var values by rememberSaveable { mutableStateOf<List<String>>(ArrayList(numbers(original?.label))) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val images = remember(context) { MealImages(context) }
    val provider: FoodProvider = remember(foods) { SavedFoodProvider(foods) }
    var query by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf<String?>(null) }
    var draftPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var photoChanged by rememberSaveable { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var savedPhoto by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(original?.id) {
        withContext(Dispatchers.IO) { images.prune() }
        if (original != null) {
            try { savedPhoto = loadPhoto(original.id) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { error = "Couldn’t display the saved photo." }
        }
    }
    fun close() {
        draftPhoto?.let { images.draftFile(it).delete() }
        onClose()
    }
    fun select(food: Food) {
        foodId = food.id
        name = food.label.name
        unit = food.label.unit
        prep = food.label.preparation
        qty = if (unit == "Grams") "100" else "1"
        values = ArrayList(numbers(food.label))
        barcode = food.barcode
        picking = false
    }
    fun useBarcode(code: String) {
        val match = provider.lookup(code)
        if (match == null) {
            barcode = code
            picking = false
        } else select(match)
    }
    ModalBottomSheet(
        onDismissRequest = { if (!busy && !processing) close() },
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = nourishColors.canvas,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(date, style = NourishType.eyebrow, color = nourishColors.muted)
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Heading(if (original != null) "Edit food" else "Log food", Modifier.weight(1f))
                TextButton(enabled = !busy && !processing, onClick = ::close) { Text("Close") }
            }
            PhotoControls(images, draftPhoto, savedPhoto.takeUnless { photoChanged }, picking,
                onPhoto = { new ->
                    draftPhoto?.let { images.draftFile(it).delete() }
                    draftPhoto = new
                    photoChanged = true
                    picking = false
                }, onBarcode = ::useBarcode, onWorking = { processing = it })
            if (picking) {
                OutlinedTextField(query, { query = it.take(100) }, Modifier.fillMaxWidth(),
                    label = { Text("Search saved foods or type barcode") }, singleLine = true)
                val matches = remember(provider, query) { provider.search(query) }
                productCode(query)?.let { code ->
                    Button(onClick = { useBarcode(code) }) { Text("Use barcode") }
                }
                Button(onClick = { picking = false }) { Text("Create a custom food") }
                if (query.isNotBlank() && matches.isEmpty()) Text("No matching food on this device.")
                matches.groupBy { it.favorite }.forEach { (starred, group) ->
                    Heading(if (starred) "Favorites" else "Recently used", style = NourishType.heading)
                    group.forEach { food ->
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(modifier = Modifier.weight(1f), onClick = { select(food) }) { Text(food.label.name) }
                            TextButton(onClick = { onFavorite(food) }) { Text(if (starred) "Unfavorite" else "Favorite") }
                        }
                    }
                }
            } else {
                barcode?.let { Text("Package · $it", style = NourishType.caption) }
                if (barcode != null && provider.lookup(barcode!!) == null) Text("New barcode. Save the label once to reuse it next time.")
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Food name") }, singleLine = true)
                Choices("Meal", meal, mealTypes) { meal = it }
                Choices("Nutrition basis", unit, portionUnits) {
                    if (it != unit) { unit = it; qty = if (it == "Grams") "100" else "1"; values = ArrayList(List(4) { "" }) }
                }
                Choices("Preparation", prep, preparations) { prep = it }
                Heading(if (unit == "Grams") "Label per 100 g" else "Label per serving", style = NourishType.heading)
                Text("Enter the values from your food label or your own estimate.")
                listOf("Energy (kcal)", "Protein (g)", "Carbs (g)", "Fat (g)").forEachIndexed { i, title ->
                    NumberField(title, values[i]) { v -> values = ArrayList(values).apply { set(i, v) } }
                }
                NumberField(if (unit == "Grams") "Grams eaten" else "Servings eaten", qty) { qty = it }
                val amounts = values.map(::parseAmount)
                val quantity = parseAmount(qty)
                val valid = name.isNotBlank() && amounts.all { it != null } && quantity != null && quantity > 0
                val draft = if (valid) Entry(entryId,date,meal,foodId,
                    Label(name.trim(),unit,prep,amounts[0]!!,amounts[1]!!,amounts[2]!!,amounts[3]!!),
                    quantity, original?.created ?: System.currentTimeMillis()) else null
                draft?.let { Text("Portion total · ${listOf(it).nutrition().calories} kcal", style = NourishType.strong) }
                if (error.isNotEmpty()) Text(error)
                Button(enabled = !busy && !processing, onClick = {
                    if (draft == null) error = "Enter a name, four label values (zero is fine), and a positive portion. Use up to three decimal places."
                    else if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                val jpeg = withContext(Dispatchers.IO) { draftPhoto?.let { images.draftFile(it).readBytes() } }
                                onSave(draft, barcode, photoChanged, jpeg)
                                close()
                            }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { error = "Couldn’t save this food. Your draft is here to retry." }
                            finally { busy = false }
                        }
                    }
                }) { Text(if (busy) "Saving…" else "Save food") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(10)) }, Modifier.fillMaxWidth(), label = { Text(label) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun Choices(title: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("$title · $value ▾") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onChange(it); open = false }) }
        }
    }
}

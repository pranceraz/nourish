package com.nourish.app.diary

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

internal data class Nutrient(val number: String, val name: String, val unit: String, val group: String)
internal data class SurveyFood(val id: String, val name: String, val amounts: Map<String, BigDecimal>)
internal data class Portion(val food: SurveyFood, val grams: BigDecimal, val assumption: String = "")

internal class SurveyCatalog(context: Context) {
    val nutrients = context.assets.open("nutrients.tsv").bufferedReader().useLines { lines ->
        lines.drop(1).map { line -> line.split('\t').let { Nutrient(it[0], it[1], it[2], it[3]) } }.toList()
    }
    val foods = context.assets.open("survey_foods.tsv").bufferedReader().useLines { lines ->
        lines.drop(1).map { line ->
            val fields = line.split('\t')
            SurveyFood(fields[0], fields[1], nutrients.mapIndexedNotNull { i, nutrient ->
                fields[i + 2].toBigDecimalOrNull()?.let { nutrient.number to it }
            }.toMap())
        }.toList()
    }
    val byId = foods.associateBy { it.id }
    fun search(query: String): List<SurveyFood> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return foods.filter { f -> words.all { f.name.contains(it, ignoreCase = true) } }
            .sortedWith(compareBy<SurveyFood> { !it.name.startsWith(words.first(), ignoreCase = true) }.thenBy { it.name.length })
            .take(8)
    }

    companion object {
        @Volatile private var cached: SurveyCatalog? = null
        fun get(context: Context): SurveyCatalog = cached ?: synchronized(this) {
            cached ?: SurveyCatalog(context.applicationContext).also { cached = it }
        }
    }
}

internal fun nutrientAmount(portions: List<Portion>, number: String): BigDecimal? {
    var total = BigDecimal.ZERO
    for (portion in portions) {
        val per100 = portion.food.amounts[number] ?: return null
        total += (per100 * portion.grams).movePointLeft(2)
    }
    return total
}

internal fun BigDecimal.display(): String = setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

internal fun portionLabel(name: String, portions: List<Portion>, perServing: Boolean): Label {
    val first = portions.first()
    fun amount(key: String) = if (perServing) nutrientAmount(portions, key) else first.food.amounts[key]
    fun milli(key: String) = requireNotNull(amount(key)).movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact()
    val nutrients = JSONObject()
    val partial = JSONObject()
    portions.flatMap { it.food.amounts.keys }.distinct().forEach { key ->
        val known = portions.filter { key in it.food.amounts }
        val value = amount(key) ?: nutrientAmount(known,key)
        value?.let { nutrients.put(key,it.toPlainString()) }
        if (known.size < portions.size) partial.put(key,true)
    }
    val ingredients = JSONArray()
    portions.forEach {
        ingredients.put(JSONObject().put("id", it.food.id).put("food", it.food.name)
            .put("grams", it.grams.toPlainString()).put("assumption", it.assumption))
    }
    val details = JSONObject().put("source", "USDA Survey Foods (FNDDS)").put("nutrients", nutrients)
        .put("ingredients", ingredients).put("partial",partial)
    return Label(name.trim().take(100), if (perServing) "Serving" else "Grams", "Unspecified",
        milli("208"), milli("203"), milli("205"), milli("204"), details.toString())
}

internal fun Entry.amount(number: String): BigDecimal? {
    val perBasis = when (number) {
        "208" -> BigDecimal.valueOf(label.kcal, 3)
        "203" -> BigDecimal.valueOf(label.protein, 3)
        "205" -> BigDecimal.valueOf(label.carbs, 3)
        "204" -> BigDecimal.valueOf(label.fat, 3)
        else -> label.details?.let { JSONObject(it).optJSONObject("nutrients")?.optString(number)?.toBigDecimalOrNull() }
    } ?: return null
    return perBasis * BigDecimal.valueOf(quantity, if (label.unit == "Grams") 5 else 3)
}

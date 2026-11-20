package com.nourish.app.diary

import android.content.Context
import java.time.LocalDate
import org.json.JSONObject
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nourish.app.model.Nutrition
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { listOf(SharedPreferencesMigration(it, "nourish", setOf("show_demo"))) },
)

internal data class Preferences(val demo: Boolean = false, val goals: Nutrition = Nutrition(),
    val hidden: Set<String> = emptySet(), val datedGoals: String = "{}") {
    fun target(number: String, date: LocalDate): String? {
        val history = JSONObject(datedGoals).optJSONObject(number)
        val latest = history?.keys()?.asSequence()?.filter { it <= date.toString() }?.maxOrNull()
        if (latest != null) return history.optString(latest).ifBlank { null }
        return when (number) {
            "208" -> goals.calories; "203" -> goals.protein; "205" -> goals.carbs; "204" -> goals.fat
            else -> 0
        }.takeIf { it > 0 }?.toString()
    }
}

internal class DiaryPreferences(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val hidden = stringSetPreferencesKey("hidden_nutrients")
    private val targets = stringPreferencesKey("dated_targets")
    private val demoKey = booleanPreferencesKey("show_demo")
    private val keys = listOf("energy", "protein", "carbs", "fat").map(::intPreferencesKey)
    val values = store.data.map { data ->
        Preferences(data[demoKey] ?: false, Nutrition(
            data[keys[0]] ?: 0, data[keys[1]] ?: 0, data[keys[2]] ?: 0, data[keys[3]] ?: 0,
        ), data[hidden].orEmpty(), data[targets] ?: "{}")
    }
    suspend fun visible(number: String, show: Boolean) {
        store.edit { data ->
            val old = data[hidden].orEmpty()
            data[hidden] = if (show) old - number else old + number
        }
    }
    suspend fun target(number: String, date: LocalDate, amount: String) {
        require(amount.isBlank() || (amount.toBigDecimalOrNull()?.signum() ?: 0) > 0)
        store.edit { data ->
            val json = JSONObject(data[targets] ?: "{}")
            val history = json.optJSONObject(number) ?: JSONObject()
            history.put(date.toString(),amount)
            data[targets] = json.put(number,history).toString()
        }
    }
    suspend fun demo(enabled: Boolean) { store.edit { it[demoKey] = enabled } }
    suspend fun goals(values: List<Int>) {
        require(values.size == keys.size && values.all { it in 0..99999 })
        store.edit { data -> keys.zip(values).forEach { (key, value) -> data[key] = value } }
    }
}

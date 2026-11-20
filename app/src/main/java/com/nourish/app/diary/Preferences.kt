package com.nourish.app.diary

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nourish.app.model.Nutrition
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { listOf(SharedPreferencesMigration(it, "nourish", setOf("show_demo"))) },
)

internal data class Preferences(val demo: Boolean = false, val goals: Nutrition = Nutrition())

internal class DiaryPreferences(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val demoKey = booleanPreferencesKey("show_demo")
    private val keys = listOf("energy", "protein", "carbs", "fat").map(::intPreferencesKey)
    val values = store.data.map { data ->
        Preferences(data[demoKey] ?: false, Nutrition(
            data[keys[0]] ?: 0, data[keys[1]] ?: 0, data[keys[2]] ?: 0, data[keys[3]] ?: 0,
        ))
    }
    suspend fun demo(enabled: Boolean) { store.edit { it[demoKey] = enabled } }
    suspend fun goals(values: List<Int>) {
        require(values.size == keys.size && values.all { it in 0..99999 })
        store.edit { data -> keys.zip(values).forEach { (key, value) -> data[key] = value } }
    }
}

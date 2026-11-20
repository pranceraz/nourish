package com.nourish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.nourish.app.ui.NourishApp
import java.time.LocalDate
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences("nourish", MODE_PRIVATE)
        setContent {
            val date by produceState(LocalDate.now()) {
                while (true) {
                    delay(30_000)
                    value = LocalDate.now()
                }
            }
            NourishApp(
                date = date,
                initiallyShowDemo = preferences.getBoolean("show_demo", true),
                onDemoPreferenceChanged = { show ->
                    preferences.edit().putBoolean("show_demo", show).apply()
                },
            )
        }
    }
}

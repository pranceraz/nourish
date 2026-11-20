package com.nourish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.nourish.app.diary.DiaryDatabase
import com.nourish.app.diary.DiaryPreferences
import com.nourish.app.ui.NourishApp
import java.time.LocalDate
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = DiaryPreferences(this)
        val dao = DiaryDatabase.open(this).diary()
        setContent {
            val date by produceState(LocalDate.now()) {
                while (true) {
                    delay(30_000)
                    value = LocalDate.now()
                }
            }
            NourishApp(date = date, dao = dao, store = preferences)
        }
    }
}

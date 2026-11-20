package com.nourish.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nourish.app.diary.ChatSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private var incoming: Job? = null

    override fun onStart() {
        super.onStart()
        incoming = lifecycleScope.launch {
            while (true) {
                try { ChatSync.receive(applicationContext) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { /* Retry when the network or account becomes available. */ }
                delay(30_000)
            }
        }
    }

    override fun onStop() {
        incoming?.cancel()
        super.onStop()
    }

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

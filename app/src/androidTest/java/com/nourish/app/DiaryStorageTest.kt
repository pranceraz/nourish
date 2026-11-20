package com.nourish.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.nourish.app.diary.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DiaryStorageTest {
    @Test fun reopenPreservesSnapshotsAndFavoritesAndRetriesSaveOnce() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "diary-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(ctx, DiaryDatabase::class.java, file).build()
        val day = "2026-07-03"
        val entry = Entry("a",day,"Snack","oats",Label("Oats","Grams","Raw",200000,10000,30000,4000),50000,1)
        try {
            val first = open()
            try {
                first.diary().apply {
                    save(entry,false)
                    save(entry,false)
                    duplicate("a","b")
                    duplicate("a","b")
                    favorite("oats",true)
                }
            } finally { first.close() }
            val reopened = open()
            try {
                reopened.diary().apply {
                    assertEquals(2,entries(day,day).first().size)
                    save(entry.copy(label = entry.label.copy(kcal = 400000)),true)
                    assertEquals(300,entries(day,day).first().nutrition().calories)
                    assertTrue(foods().first().single().favorite)
                    delete("a")
                    assertEquals(100,entries(day,day).first().nutrition().calories)
                    assertTrue(entries("2026-07-02","2026-07-02").first().isEmpty())
                    delete("b")
                    assertTrue(entries(day,day).first().isEmpty())
                }
            } finally { reopened.close() }
        } finally { ctx.deleteDatabase(file) }
        Unit
    }
}

package com.nourish.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.nourish.app.diary.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class InputMigrationTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    @get:Rule val migrations = MigrationTestHelper(instrument, DiaryDatabase::class.java)

    @Test fun p1DiarySurvivesAndPhotoLifecycleIsAtomic() = runBlocking {
        val file = "migration-${UUID.randomUUID()}"
        migrations.createDatabase(file, 1).apply {
            execSQL("INSERT INTO foods VALUES ('f',1,1,'Milk','Serving','Unspecified',120000,8000,12000,5000)")
            execSQL("INSERT INTO entries VALUES ('e','2026-01-01','Lunch','f',1000,1,'Milk','Serving','Unspecified',120000,8000,12000,5000)")
            close()
        }
        val db = Room.databaseBuilder(instrument.targetContext, DiaryDatabase::class.java, file).build()
        try {
            db.diary().apply {
                val existing = requireNotNull(entry("e"))
                assertEquals(120000L, existing.label.kcal)
                assertTrue(requireNotNull(food("f")).favorite)
                assertNull(photo("e"))
                val bytes = byteArrayOf(1,2,3)
                saveWithInputs(existing,true,"036000291452",true,bytes)
                assertEquals("00036000291452", food("f")?.barcode)
                duplicate("e","copy")
                assertArrayEquals(bytes, photo("copy"))
                saveWithInputs(existing,true,null,true,null)
                assertNull(photo("e"))
                delete("copy")
                assertNull(photo("copy"))
            }
        } finally { db.close(); instrument.targetContext.deleteDatabase(file) }
        Unit
    }
}

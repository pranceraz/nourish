package com.nourish.app

import androidx.room.Room
import android.content.Context
import org.json.JSONObject
import androidx.test.platform.app.InstrumentationRegistry
import com.nourish.app.diary.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InputMigrationTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private fun createP1Database(file: String) {
        val asset = "com.nourish.app.diary.DiaryDatabase/1.json"
        val schema = instrument.context.assets.open(asset).bufferedReader().use { JSONObject(it.readText()) }
            .getJSONObject("database")
        instrument.targetContext.openOrCreateDatabase(file, Context.MODE_PRIVATE, null).use { db ->
            val tables = schema.getJSONArray("entities")
            for (index in 0 until tables.length()) {
                val entity = tables.getJSONObject(index)
                fun sql(template: String) = template.replace("\${TABLE_NAME}", entity.getString("tableName"))
                db.execSQL(sql(entity.getString("createSql")))
                val indices = entity.optJSONArray("indices") ?: continue
                for (i in 0 until indices.length()) db.execSQL(sql(indices.getJSONObject(i).getString("createSql")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
            db.execSQL("INSERT INTO foods VALUES ('f',1,1,'Milk','Serving','Unspecified',120000,8000,12000,5000)")
            db.execSQL("INSERT INTO entries VALUES ('e','2026-01-01','Lunch','f',1000,1,'Milk','Serving','Unspecified',120000,8000,12000,5000)")
            db.version = schema.getInt("version")
        }
    }

    @Test fun p1DiarySurvivesAndPhotoLifecycleIsAtomic() = runBlocking {
        val file = "migration-${UUID.randomUUID()}"
        createP1Database(file)
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

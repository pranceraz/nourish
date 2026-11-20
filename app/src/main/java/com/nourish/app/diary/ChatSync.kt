package com.nourish.app.diary

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

internal data class IncomingMeal(val sequence: Long, val entries: List<Entry>)

internal fun unpackMeal(record: JSONObject, created: Long): IncomingMeal {
    val receipt = UUID.fromString(record.getString("id")).toString()
    val meal = record.getJSONObject("meal")
    val date = LocalDate.parse(meal.getString("date")).toString()
    val type = meal.getString("meal")
    require(type in mealTypes)
    val rows = meal.getJSONArray("ingredients")
    require(rows.length() in 1..20)
    val ingredients = (0 until rows.length()).map(rows::getJSONObject)
    val dish = when (meal.getString("mode")) {
        "dish" -> true
        "ingredients" -> false
        else -> error("Unknown meal format")
    }
    val groups = if (dish) listOf(ingredients) else ingredients.map { listOf(it) }
    val entries = groups.mapIndexed { index, group ->
        val first = group.first()
        val source = if (dish) group.map { it.getString("source") }.distinct().joinToString(" · ") else first.getString("source")
        val values = if (dish) meal.getJSONObject("totals") else first.getJSONObject("per100")
        fun milli(number: String): Long = BigDecimal(values.getString(number)).movePointRight(3)
            .setScale(0,RoundingMode.HALF_UP).longValueExact()
        val details = JSONObject().put("source",source).put("receipt",receipt).put("nutrients",values)
        val partial = JSONObject()
        if (dish) meal.getJSONArray("partial").let { keys ->
            for (i in 0 until keys.length()) partial.put(keys.getString(i),true)
        }
        details.put("partial",partial).put("ingredients",JSONArray().apply {
            group.forEach { item -> put(JSONObject().put("food",item.getString("food"))
                .put("id",item.getString("source")).put("source",item.getString("source"))
                .put("grams",item.getString("grams")).put("assumption",item.getString("assumption"))) }
        })
        val label = Label(if (dish) meal.getString("name") else first.getString("food"),
            if (dish) "Serving" else "Grams","Unspecified",milli("208"),milli("203"),milli("205"),milli("204"),details.toString())
        val quantity = if (dish) 1000L else BigDecimal(first.getString("grams")).movePointRight(3).longValueExact()
        val id = "chat-$receipt-$index"
        Entry(id,date,type,id,label,quantity,created+index)
    }
    return IncomingMeal(record.getLong("seq"),entries)
}

internal object ChatSync {
    private val lock = Mutex()

    suspend fun receive(context: Context): Int = lock.withLock {
        val service = DraftConnection(context)
        if (!service.chatEnabled) return@withLock 0
        val session = service.sessionId
        val dao = DiaryDatabase.open(context).diary()
        var count = 0
        while (service.chatEnabled) {
            val cursor = dao.chatCursor()?.sequence ?: 0
            val page = service.request("/v1/diary/incoming?after=$cursor","GET")
            val rows = page.getJSONArray("meals")
            val meals = (0 until rows.length()).map { unpackMeal(rows.getJSONObject(it), System.currentTimeMillis()) }
            if (!service.chatEnabled || service.sessionId != session) break
            count += dao.receiveChat(page.getString("account"),meals)
            if (rows.length() < 20) break
        }
        count
    }
}

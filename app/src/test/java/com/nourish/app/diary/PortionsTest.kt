package com.nourish.app.diary

import org.junit.Assert.*
import org.junit.Test

class PortionsTest {
    @Test fun totalsRoundAfterSummingGramsAndServings() {
        val food = Label("Beans", "Grams", "Cooked", 120000, 8000, 14000, 1000)
        val grams = Entry("a","2026-10-01","Lunch","f",food,12500,0)
        val serving = grams.copy(label = food.copy(unit = "Serving"), quantity = 125)
        assertEquals(30, listOf(grams, serving).nutrition().calories)
        assertEquals(4, listOf(grams, serving).nutrition().carbs)
        assertEquals(0, listOf(grams, serving).nutrition().fat)
    }

    @Test fun amountsAreExactAndBounded() {
        assertEquals(1250L, parseAmount("1,250"))
        assertEquals("1.25", amountText(1250))
        listOf("-1", "1e6", "0.0001", "", "100000", "NaN").forEach { assertNull(parseAmount(it)) }
    }
}

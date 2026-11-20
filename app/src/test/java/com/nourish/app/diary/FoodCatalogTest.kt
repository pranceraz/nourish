package com.nourish.app.diary

import org.junit.Assert.*
import org.junit.Test

class FoodCatalogTest {
    @Test fun equivalentCodesUseOneProduct() {
        assertEquals("00036000291452", productCode("036000291452"))
        assertEquals(productCode("036000291452"), productCode("0036000291452"))
        listOf("", "1234", "036000291453", "036O00291452").forEach { assertNull(productCode(it)) }
    }
    @Test fun searchPreservesFavoriteOrderAndMatchesAllTerms() {
        val oats = Food("a", Label("Rolled brown oats", "Grams", "Raw", 1,2,3,4), 0, true, "00036000291452")
        val other = oats.copy(id = "b", label = oats.label.copy(name = "Brown rice"), favorite = false, barcode = null)
        val catalog = SavedFoodProvider(listOf(oats, other))
        assertEquals(listOf(oats, other), catalog.search(" Brown "))
        assertEquals(listOf(oats), catalog.search("OATS rolled"))
        assertEquals(listOf(oats), catalog.search("036000291452"))
        assertEquals(oats, catalog.lookup("0036000291452"))
        assertNull(catalog.lookup("invalid"))
    }
}

package com.nourish.app.diary

/** Normalize UPC/EAN to GTIN-14 so a scanned UPC and its leading-zero EAN find the same food. */
internal fun productCode(raw: String): String? {
    val digits = raw.trim()
    if (digits.length !in setOf(8, 12, 13, 14) || digits.any { it !in '0'..'9' }) return null
    val sum = digits.reversed().mapIndexed { index, char ->
        char.digitToInt() * if (index % 2 == 1) 3 else 1
    }.sum()
    return digits.padStart(14, '0').takeIf { sum % 10 == 0 }
}

internal interface FoodProvider {
    fun search(query: String): List<Food>
    fun lookup(barcode: String): Food?
}

internal class SavedFoodProvider(private val foods: List<Food>) : FoodProvider {
    override fun lookup(barcode: String): Food? =
        productCode(barcode)?.let { code -> foods.find { it.barcode == code } }

    override fun search(query: String): List<Food> {
        val words = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val code = productCode(query)
        return foods.filter { food ->
            (code != null && food.barcode == code) || words.all { food.label.name.contains(it, ignoreCase = true) }
        }
    }
}

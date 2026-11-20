package com.nourish.app.diary

import android.content.Context
import androidx.room.*
import com.nourish.app.model.Nutrition
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.Flow

internal val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")
internal val preparations = listOf("Unspecified", "Raw", "Cooked")
internal val portionUnits = listOf("Serving", "Grams")

// User-entered values use thousandths, with no floating-point or locale-dependent storage.
internal fun parseAmount(text: String): Long? {
    if (!Regex("""\d{1,5}(?:[.,]\d{1,3})?""").matches(text.trim())) return null
    return text.trim().replace(',', '.').toBigDecimal().movePointRight(3).longValueExact()
}

internal fun amountText(value: Long): String =
    BigDecimal.valueOf(value, 3).stripTrailingZeros().toPlainString()

internal data class Label(
    val name: String, val unit: String, val preparation: String,
    val kcal: Long, val protein: Long, val carbs: Long, val fat: Long,
)

@Entity(tableName = "foods")
internal data class Food(
    @PrimaryKey val id: String, @Embedded val label: Label,
    val lastUsed: Long, val favorite: Boolean = false,
)

@Entity(tableName = "entries", indices = [Index("date"), Index("foodId")])
internal data class Entry(
    @PrimaryKey val id: String, val date: String, val meal: String, val foodId: String,
    @Embedded val label: Label, val quantity: Long, val created: Long,
)

internal fun List<Entry>.nutrition(): Nutrition {
    fun sum(nutrient: (Label) -> Long): Int {
        val total = fold(BigDecimal.ZERO) { acc, entry ->
            val value = BigDecimal.valueOf(nutrient(entry.label), 3)
            val count = BigDecimal.valueOf(entry.quantity, if (entry.label.unit == "Grams") 5 else 3)
            acc + value * count
        }
        return total.setScale(0, RoundingMode.HALF_UP).min(Int.MAX_VALUE.toBigDecimal()).toInt()
    }
    return Nutrition(sum { it.kcal }, sum { it.protein }, sum { it.carbs }, sum { it.fat })
}

@Dao
internal abstract class DiaryDao {
    @Query("SELECT * FROM entries WHERE date BETWEEN :from AND :through ORDER BY date, created, id")
    abstract fun entries(from: String, through: String): Flow<List<Entry>>
    @Query("SELECT * FROM foods ORDER BY favorite DESC, lastUsed DESC, id")
    abstract fun foods(): Flow<List<Food>>
    @Query("SELECT * FROM entries WHERE id = :id")
    abstract suspend fun entry(id: String): Entry?
    @Query("SELECT * FROM foods WHERE id = :id")
    abstract suspend fun food(id: String): Food?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entry: Entry): Long
    @Update abstract suspend fun update(entry: Entry)
    @Upsert abstract suspend fun storeFood(food: Food)
    @Query("UPDATE foods SET favorite = :enabled WHERE id = :id")
    abstract suspend fun favorite(id: String, enabled: Boolean)
    @Query("DELETE FROM entries WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Transaction
    open suspend fun save(value: Entry, editing: Boolean) {
        val existing = entry(value.id)
        if (!editing && existing != null) return
        require(!editing || existing != null) { "This entry was removed." }
        require(value.quantity in 1..99_999_999 && value.meal in mealTypes)
        val label = value.label
        require(label.name.isNotBlank() && label.name.length <= 100 && label.unit in portionUnits &&
            label.preparation in preparations && listOf(label.kcal, label.protein, label.carbs, label.fat).all { it in 0..99_999_999 })
        val catalog = food(value.foodId)
        storeFood(Food(value.foodId, label, System.currentTimeMillis(), catalog?.favorite ?: false))
        if (editing) update(value) else insert(value)
    }

    @Transaction
    open suspend fun duplicate(id: String, newId: String) {
        val entry = requireNotNull(entry(id))
        val now = System.currentTimeMillis()
        if (insert(entry.copy(id = newId, created = now)) != -1L) {
            food(entry.foodId)?.let { storeFood(it.copy(lastUsed = now)) }
        }
    }
}

@Database(entities = [Entry::class, Food::class], version = 1, exportSchema = true)
internal abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diary(): DiaryDao
    companion object {
        @Volatile private var instance: DiaryDatabase? = null
        fun open(context: Context): DiaryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DiaryDatabase::class.java, "diary.db")
                .build().also { instance = it }
        }
    }
}

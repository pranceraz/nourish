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
    val details: String? = null,
)

@Entity(tableName = "foods", indices = [Index(value = ["barcode"], unique = true)])
internal data class Food(
    @PrimaryKey val id: String, @Embedded val label: Label,
    val lastUsed: Long, val favorite: Boolean = false, val barcode: String? = null,
)

@Entity(tableName = "entries", indices = [Index("date"), Index("foodId")])
internal data class Entry(
    @PrimaryKey val id: String, val date: String, val meal: String, val foodId: String,
    @Embedded val label: Label, val quantity: Long, val created: Long,
)

@Entity(tableName = "meal_photos", foreignKeys = [ForeignKey(
    entity = Entry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE,
)])
internal class MealPhoto(@PrimaryKey val entryId: String, val jpeg: ByteArray)

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

@Entity(tableName = "chat_cursor")
internal data class ChatCursor(@PrimaryKey val slot: Int, val account: String, val sequence: Long)

@Dao
internal abstract class DiaryDao {
    @Query("SELECT * FROM chat_cursor WHERE slot=1")
    abstract suspend fun chatCursor(): ChatCursor?
    @Upsert abstract suspend fun setChatCursor(cursor: ChatCursor)

    @Transaction
    open suspend fun receiveChat(account: String, meals: List<IncomingMeal>): Int {
        val existing = chatCursor()
        if (existing != null && existing.account != account) {
            throw ServiceError("This diary is linked to a different account. Reconnect to the original account to receive meals.")
        }
        var sequence = existing?.sequence ?: 0
        var received = 0
        for (meal in meals) {
            if (meal.sequence <= sequence) continue
            saveMeal(meal.entries,null)
            sequence = meal.sequence
            received++
        }
        if (existing == null || received > 0) setChatCursor(ChatCursor(1,account,sequence))
        return received
    }

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

    @Query("SELECT jpeg FROM meal_photos WHERE entryId = :id")
    abstract suspend fun photo(id: String): ByteArray?
    @Upsert abstract suspend fun storePhoto(photo: MealPhoto)
    @Query("DELETE FROM meal_photos WHERE entryId = :id")
    abstract suspend fun removePhoto(id: String)
    @Query("UPDATE foods SET barcode = :code WHERE id = :foodId")
    abstract suspend fun bindBarcode(foodId: String, code: String)

    @Transaction
    open suspend fun saveWithInputs(value: Entry, editing: Boolean, barcode: String?, photoChanged: Boolean, jpeg: ByteArray?) {
        if (!editing && entry(value.id) != null) return
        save(value, editing)
        if (barcode != null) bindBarcode(value.foodId, requireNotNull(productCode(barcode)))
        if (photoChanged) {
            if (jpeg == null) removePhoto(value.id) else storePhoto(MealPhoto(value.id, jpeg))
        }
    }

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
        // A quantity-only edit must not replace a newer reusable food label.
        val catalogLabel = if (catalog != null && existing?.label == label) catalog.label else label
        storeFood(Food(value.foodId, catalogLabel, System.currentTimeMillis(), catalog?.favorite ?: false, catalog?.barcode))
        if (editing) update(value) else insert(value)
    }

    @Transaction
    open suspend fun saveMeal(entries: List<Entry>, jpeg: ByteArray?) {
        require(entries.isNotEmpty())
        if (entry(entries.first().id) != null) return
        entries.forEachIndexed { index, entry ->
            saveWithInputs(entry, false, null, index == 0 && jpeg != null, if (index == 0) jpeg else null)
        }
    }

    @Transaction
    open suspend fun duplicate(id: String, newId: String) {
        val entry = requireNotNull(entry(id))
        val now = System.currentTimeMillis()
        if (insert(entry.copy(id = newId, created = now)) != -1L) {
            photo(id)?.let { storePhoto(MealPhoto(newId, it)) }
            food(entry.foodId)?.let { storeFood(it.copy(lastUsed = now)) }
        }
    }
}

@Database(entities = [Entry::class, Food::class, MealPhoto::class, ChatCursor::class], version = 4, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)])
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

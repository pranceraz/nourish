package com.nourish.app.model

internal data class Nutrition(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
) {
    operator fun plus(other: Nutrition) = Nutrition(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
    )
}

internal data class DemoFood(val name: String, val meal: String, val nutrition: Nutrition)

internal object DemoNutrition {
    val goal = Nutrition(calories = 2_100, protein = 120, carbs = 235, fat = 70)

    val starterMeals = listOf(
        DemoFood("Blueberry overnight oats", "Breakfast", Nutrition(418, 22, 54, 13)),
        DemoFood("Tahini harvest bowl", "Lunch", Nutrition(626, 36, 65, 24)),
        DemoFood("Apple & almond toast", "Snack", Nutrition(438, 25, 36, 14)),
    )

    val options = listOf(
        DemoFood("Peach yogurt cup", "Snack", Nutrition(180, 14, 23, 4)),
        DemoFood("Hummus & crackers", "Snack", Nutrition(210, 7, 27, 8)),
        DemoFood("Banana & peanut butter", "Snack", Nutrition(225, 6, 32, 9)),
    )

    private val priorDays = listOf(1_780, 2_090, 1_640, 1_915, 2_035, 1_864)

    fun meals(additions: List<Int>): List<DemoFood> =
        starterMeals + additions.map(options::get)

    fun total(meals: List<DemoFood>): Nutrition = meals.fold(Nutrition()) { sum, food ->
        sum + food.nutrition
    }

    fun week(todayCalories: Int): List<Int> = priorDays + todayCalories
}

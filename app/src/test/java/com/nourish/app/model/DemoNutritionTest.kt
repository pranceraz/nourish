package com.nourish.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoNutritionTest {
    @Test
    fun starterTotalsMatchVisibleSampleMeals() {
        assertEquals(Nutrition(1_482, 83, 155, 51), DemoNutrition.total(DemoNutrition.meals(emptyList())))
    }

    @Test
    fun additionsUpdateAllNutrientsAndTheLatestTrendDay() {
        val meals = DemoNutrition.meals(listOf(0, 2))
        val total = DemoNutrition.total(meals)

        assertEquals(listOf("Peach yogurt cup", "Banana & peanut butter"), meals.takeLast(2).map { it.name })
        assertEquals(Nutrition(1_887, 103, 210, 64), total)
        assertEquals(listOf(1_780, 2_090, 1_640, 1_915, 2_035, 1_864, 1_887), DemoNutrition.week(total.calories))
    }
}

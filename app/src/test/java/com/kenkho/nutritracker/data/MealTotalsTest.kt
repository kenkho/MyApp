package com.kenkho.nutritracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MealTotalsTest {

    private fun item(name: String, servings: Double, cal: Double, carbs: Double, sugar: Double, sodium: Double, pos: Int = 0) =
        MealItem(
            position = pos, name = name, servingSize = "", servings = servings,
            caloriesPerServing = cal, carbsPerServingG = carbs, sugarPerServingG = sugar, sodiumPerServingMg = sodium,
        )

    private val entry = MealEntry(id = 1, title = "", timestamp = 0, meal = MealType.LUNCH)

    @Test
    fun mealTotalIsSumOfItemsTimesServings() {
        val meal = MealWithItems(
            entry,
            listOf(
                item("A", servings = 1.5, cal = 230.0, carbs = 37.0, sugar = 12.0, sodium = 160.0),
                item("B", servings = 2.0, cal = 10.0, carbs = 1.0, sugar = 0.5, sodium = 5.0),
            ),
        )
        val t = meal.totals()
        assertEquals(345.0 + 20.0, t.calories, 1e-9)
        assertEquals(55.5 + 2.0, t.carbsG, 1e-9)
        assertEquals(18.0 + 1.0, t.sugarG, 1e-9)
        assertEquals(240.0 + 10.0, t.sodiumMg, 1e-9)
    }

    @Test
    fun dayTotalAddsMeals() {
        val m1 = MealWithItems(entry, listOf(item("A", 1.0, 100.0, 10.0, 1.0, 50.0)))
        val m2 = MealWithItems(entry.copy(id = 2), listOf(item("B", 1.0, 200.0, 20.0, 2.0, 100.0)))
        assertEquals(300.0, listOf(m1, m2).sumTotals().calories, 1e-9)
    }

    @Test
    fun titleFallsBackToItemNamesInOrder() {
        val m = MealWithItems(entry, listOf(item("Bread", 1.0, 0.0, 0.0, 0.0, 0.0, pos = 1), item("Soup", 1.0, 0.0, 0.0, 0.0, 0.0, pos = 0)))
        assertEquals("Soup, Bread", m.displayTitle)
        assertEquals("Lunch", MealWithItems(entry, emptyList()).displayTitle)
        assertEquals("My lunch", MealWithItems(entry.copy(title = "My lunch"), emptyList()).displayTitle)
    }
}

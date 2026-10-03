package com.kenkho.nutritracker.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {

    @Test
    fun parsesCurrentStyleLabel() {
        val text = """
            Nutrition Facts
            8 servings per container
            Serving size 2/3 cup (55g)
            Amount per serving
            Calories 230
            % Daily Value*
            Total Fat 8g 10%
            Sodium 160mg 7%
            Total Carbohydrate 37g 13%
            Dietary Fiber 4g 14%
            Total Sugars 12g
            Includes 10g Added Sugars 20%
            Protein 3g
            * The % Daily Value (DV) tells you how much a nutrient in a serving of food
            contributes to a daily diet. 2,000 calories a day is used for general nutrition advice.
        """.trimIndent()

        val p = NutritionLabelParser.parse(text)
        assertEquals("2/3 cup (55g)", p.servingSize)
        assertEquals(230.0, p.calories!!, 0.0)
        assertEquals(37.0, p.carbsG!!, 0.0)
        assertEquals(12.0, p.sugarG!!, 0.0)
        assertEquals(160.0, p.sodiumMg!!, 0.0)
    }

    @Test
    fun caloriesOnSeparateLineAndValuesOnNextLine() {
        val text = """
            Serving size
            1 bar (40g)
            Calories
            190
            Sodium
            1,200mg
            Total Carb.
            25g
            Sugars 9g
        """.trimIndent()

        val p = NutritionLabelParser.parse(text)
        assertEquals("1 bar (40g)", p.servingSize)
        assertEquals(190.0, p.calories!!, 0.0)
        assertEquals(1200.0, p.sodiumMg!!, 0.0)
        assertEquals(25.0, p.carbsG!!, 0.0)
        assertEquals(9.0, p.sugarG!!, 0.0)
    }

    @Test
    fun olderLabelIgnoresCaloriesFromFatAndAddedSugars() {
        val text = """
            Calories 110 Calories from Fat 15
            Sodium 0mg 0%
            Total Carbohydrate 23g 8%
            Added Sugars 4g
            Sugars 5g
        """.trimIndent()

        val p = NutritionLabelParser.parse(text)
        assertEquals(110.0, p.calories!!, 0.0)
        assertEquals(0.0, p.sodiumMg!!, 0.0)
        assertEquals(5.0, p.sugarG!!, 0.0)
    }

    @Test
    fun fixesLetterOReadAsZero() {
        val p = NutritionLabelParser.parse("Total Sugars Og\nSodium Omg")
        assertEquals(0.0, p.sugarG!!, 0.0)
        assertEquals(0.0, p.sodiumMg!!, 0.0)
    }

    @Test
    fun sodiumInGramsIsConvertedToMilligrams() {
        val p = NutritionLabelParser.parse("Sodium 1.2g")
        assertEquals(1200.0, p.sodiumMg!!, 0.001)
    }

    @Test
    fun footnoteCaloriesAreNotTreatedAsValue() {
        val p = NutritionLabelParser.parse("2,000 calories a day is used for general nutrition advice.")
        assertNull(p.calories)
    }

    @Test
    fun unrelatedTextFindsNothing() {
        assertFalse(NutritionLabelParser.parse("Hello world\nIngredients: oats, honey").foundAnything)
    }

    @Test
    fun orElseFillsMissingFields() {
        val merged = ParsedLabel(calories = 100.0).orElse(ParsedLabel(calories = 5.0, sugarG = 3.0))
        assertEquals(100.0, merged.calories!!, 0.0)
        assertEquals(3.0, merged.sugarG!!, 0.0)
    }
}

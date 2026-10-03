package com.kenkho.nutritracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack");

    companion object {
        /** Default meal tag for a new entry, based on the time of day. */
        fun forHour(hour: Int): MealType = when (hour) {
            in 5..10 -> BREAKFAST
            in 11..14 -> LUNCH
            in 17..20 -> DINNER
            else -> SNACK
        }
    }
}

/**
 * One diary entry. Nutrient values are stored per serving exactly as printed on the
 * label, and [servings] is how many servings were eaten; totals are derived from both.
 */
@Entity(tableName = "food_entries", indices = [Index("timestamp")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Epoch milliseconds. */
    val timestamp: Long,
    val meal: MealType,
    /** Serving size text from the label, e.g. "2/3 cup (55g)". */
    val servingSize: String,
    val servings: Double,
    val caloriesPerServing: Double,
    val carbsPerServingG: Double,
    val sugarPerServingG: Double,
    val sodiumPerServingMg: Double,
    val notes: String = "",
)

data class NutrientTotals(
    val calories: Double = 0.0,
    val carbsG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
) {
    operator fun plus(other: NutrientTotals) = NutrientTotals(
        calories + other.calories,
        carbsG + other.carbsG,
        sugarG + other.sugarG,
        sodiumMg + other.sodiumMg,
    )
}

fun FoodEntry.totals() = NutrientTotals(
    calories = caloriesPerServing * servings,
    carbsG = carbsPerServingG * servings,
    sugarG = sugarPerServingG * servings,
    sodiumMg = sodiumPerServingMg * servings,
)

fun List<FoodEntry>.sumTotals(): NutrientTotals =
    fold(NutrientTotals()) { acc, e -> acc + e.totals() }

fun FoodEntry.localDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)

fun FoodEntry.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    localDateTime(zone).toLocalDate()

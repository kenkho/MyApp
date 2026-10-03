package com.kenkho.nutritracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
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

/** One diary entry: a meal eaten at a point in time, made up of one or more [MealItem]s. */
@Entity(tableName = "meal_entries", indices = [Index("timestamp")])
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Optional name for the whole meal, e.g. "Chicken salad". */
    val title: String,
    /** Epoch milliseconds. */
    val timestamp: Long,
    val meal: MealType,
    val notes: String = "",
)

/**
 * A food or ingredient within a meal. Nutrient values are stored per serving exactly as
 * printed on the label, and [servings] is how many servings went into the meal.
 */
@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class MealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long = 0,
    /** Order of the item within the meal. */
    val position: Int = 0,
    val name: String,
    /** Serving size text from the label, e.g. "2/3 cup (55g)". */
    val servingSize: String,
    val servings: Double,
    val caloriesPerServing: Double,
    val carbsPerServingG: Double,
    val sugarPerServingG: Double,
    val sodiumPerServingMg: Double,
)

data class MealWithItems(
    @Embedded val entry: MealEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val items: List<MealItem>,
) {
    val sortedItems: List<MealItem> get() = items.sortedBy { it.position }

    /** The meal's own title, or its item names when no title was given. */
    val displayTitle: String
        get() = entry.title.ifBlank { sortedItems.joinToString(", ") { it.name }.ifBlank { entry.meal.label } }
}

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

fun MealItem.totals() = NutrientTotals(
    calories = caloriesPerServing * servings,
    carbsG = carbsPerServingG * servings,
    sugarG = sugarPerServingG * servings,
    sodiumMg = sodiumPerServingMg * servings,
)

fun MealWithItems.totals(): NutrientTotals =
    items.fold(NutrientTotals()) { acc, item -> acc + item.totals() }

fun List<MealWithItems>.sumTotals(): NutrientTotals =
    fold(NutrientTotals()) { acc, m -> acc + m.totals() }

fun MealEntry.localDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)

fun MealEntry.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    localDateTime(zone).toLocalDate()

package com.kenkho.nutritracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kenkho.nutritracker.data.MealEntry
import com.kenkho.nutritracker.data.MealItem
import com.kenkho.nutritracker.data.MealType
import com.kenkho.nutritracker.data.MealWithItems
import com.kenkho.nutritracker.data.NutriDatabase
import com.kenkho.nutritracker.data.NutrientTotals
import com.kenkho.nutritracker.data.localDateTime
import com.kenkho.nutritracker.data.totals
import com.kenkho.nutritracker.ocr.ParsedLabel
import com.kenkho.nutritracker.toAmount
import com.kenkho.nutritracker.toInput
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Editable state for one food item in the meal; numbers are kept as typed text. */
class ItemDraft(expanded: Boolean = true) {
    /** Stable key for Compose lists, since items can be removed from the middle. */
    val key: Long = nextKey++

    var expanded by mutableStateOf(expanded)
    var name by mutableStateOf("")
    var servingSize by mutableStateOf("")
    var servings by mutableStateOf("1")
    var calories by mutableStateOf("")
    var carbs by mutableStateOf("")
    var sugar by mutableStateOf("")
    var sodium by mutableStateOf("")
    var scanMessage by mutableStateOf<String?>(null)

    val servingsValue: Double? get() = servings.toAmount()?.takeIf { it > 0 }

    val isValid: Boolean
        get() = name.isNotBlank() && servingsValue != null &&
            listOf(calories, carbs, sugar, sodium).all { it.isBlank() || (it.toAmount() ?: -1.0) >= 0 }

    /** The item as it would be saved, or null while servings is not a positive number. */
    fun toItem(): MealItem? {
        val s = servingsValue ?: return null
        return MealItem(
            name = name.trim(),
            servingSize = servingSize.trim(),
            servings = s,
            caloriesPerServing = calories.toAmount() ?: 0.0,
            carbsPerServingG = carbs.toAmount() ?: 0.0,
            sugarPerServingG = sugar.toAmount() ?: 0.0,
            sodiumPerServingMg = sodium.toAmount() ?: 0.0,
        )
    }

    /** Totals for the servings in this item, using 0 for blank fields. */
    val totals: NutrientTotals get() = toItem()?.totals() ?: NutrientTotals()

    fun applyScan(label: ParsedLabel) {
        label.servingSize?.let { servingSize = it }
        label.calories?.let { calories = it.toInput() }
        label.carbsG?.let { carbs = it.toInput() }
        label.sugarG?.let { sugar = it.toInput() }
        label.sodiumMg?.let { sodium = it.toInput() }
        val missing = buildList {
            if (label.calories == null) add("calories")
            if (label.carbsG == null) add("carbs")
            if (label.sugarG == null) add("sugar")
            if (label.sodiumMg == null) add("sodium")
        }
        scanMessage = if (missing.isEmpty()) "Label values filled in. Please check them."
        else "Not found on label: ${missing.joinToString()}. Please enter them manually."
        expanded = true
    }

    companion object {
        private var nextKey = 0L

        fun from(item: MealItem) = ItemDraft(expanded = false).apply {
            name = item.name
            servingSize = item.servingSize
            servings = item.servings.toInput()
            calories = item.caloriesPerServing.toInput()
            carbs = item.carbsPerServingG.toInput()
            sugar = item.sugarPerServingG.toInput()
            sodium = item.sodiumPerServingMg.toInput()
        }
    }
}

class EditMealViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = NutriDatabase.get(app).mealDao()

    private val mealId: Long = handle[ARG_ID] ?: NO_ID
    val isNew: Boolean = mealId == NO_ID

    var loaded by mutableStateOf(isNew)
        private set

    var title by mutableStateOf("")
    var dateTime by mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0))
    var meal by mutableStateOf(MealType.forHour(LocalTime.now().hour))
    var notes by mutableStateOf("")
    val items = mutableStateListOf(ItemDraft())

    /** The item that opened the label scanner; the scan result is written into it. */
    private var scanTarget: ItemDraft? = null

    init {
        if (isNew) {
            val epochDay: Long = handle[ARG_DATE] ?: NO_DATE
            if (epochDay != NO_DATE) {
                val day = LocalDate.ofEpochDay(epochDay)
                if (day != LocalDate.now()) dateTime = day.atTime(12, 0)
            }
        } else {
            viewModelScope.launch {
                dao.byId(mealId)?.let(::fill)
                loaded = true
            }
        }
    }

    private fun fill(m: MealWithItems) {
        title = m.entry.title
        dateTime = m.entry.localDateTime()
        meal = m.entry.meal
        notes = m.entry.notes
        items.clear()
        items += m.sortedItems.map(ItemDraft::from)
        if (items.isEmpty()) items += ItemDraft()
    }

    fun addItem() {
        items.forEach { it.expanded = false }
        items += ItemDraft()
    }

    fun removeItem(item: ItemDraft) {
        items.remove(item)
        if (items.isEmpty()) items += ItemDraft()
    }

    fun startScan(item: ItemDraft) {
        scanTarget = item
    }

    fun applyScan(label: ParsedLabel) {
        (scanTarget?.takeIf { it in items } ?: items.last()).applyScan(label)
        scanTarget = null
    }

    val totals: NutrientTotals
        get() = items.fold(NutrientTotals()) { acc, item -> acc + item.totals }

    val canSave: Boolean
        get() = items.isNotEmpty() && items.all { it.isValid }

    fun save(onDone: () -> Unit) {
        if (!canSave) return
        val entry = MealEntry(
            id = if (isNew) 0 else mealId,
            title = title.trim(),
            timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            meal = meal,
            notes = notes.trim(),
        )
        val toSave = items.mapNotNull { it.toItem() }
        viewModelScope.launch {
            dao.save(entry, toSave)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (isNew) return
        viewModelScope.launch {
            dao.delete(mealId)
            onDone()
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_DATE = "date"
        const val NO_ID = -1L
        const val NO_DATE = Long.MIN_VALUE
    }
}

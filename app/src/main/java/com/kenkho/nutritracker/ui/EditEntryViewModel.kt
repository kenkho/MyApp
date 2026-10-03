package com.kenkho.nutritracker.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kenkho.nutritracker.data.FoodEntry
import com.kenkho.nutritracker.data.MealType
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

class EditEntryViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val dao = NutriDatabase.get(app).foodEntryDao()

    private val entryId: Long = handle[ARG_ID] ?: NO_ID
    val isNew: Boolean = entryId == NO_ID

    var loaded by mutableStateOf(isNew)
        private set

    var name by mutableStateOf("")
    var dateTime by mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0))
    var meal by mutableStateOf(MealType.forHour(LocalTime.now().hour))
    var servingSize by mutableStateOf("")
    var servings by mutableStateOf("1")
    var calories by mutableStateOf("")
    var carbs by mutableStateOf("")
    var sugar by mutableStateOf("")
    var sodium by mutableStateOf("")
    var notes by mutableStateOf("")

    /** Message shown after a scan, e.g. which values were found. */
    var scanMessage by mutableStateOf<String?>(null)

    init {
        if (isNew) {
            val epochDay: Long = handle[ARG_DATE] ?: NO_DATE
            if (epochDay != NO_DATE) {
                val day = LocalDate.ofEpochDay(epochDay)
                if (day != LocalDate.now()) dateTime = day.atTime(12, 0)
            }
        } else {
            viewModelScope.launch {
                dao.byId(entryId)?.let(::fill)
                loaded = true
            }
        }
    }

    private fun fill(e: FoodEntry) {
        name = e.name
        dateTime = e.localDateTime()
        meal = e.meal
        servingSize = e.servingSize
        servings = e.servings.toInput()
        calories = e.caloriesPerServing.toInput()
        carbs = e.carbsPerServingG.toInput()
        sugar = e.sugarPerServingG.toInput()
        sodium = e.sodiumPerServingMg.toInput()
        notes = e.notes
    }

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
    }

    val servingsValue: Double? get() = servings.toAmount()?.takeIf { it > 0 }

    /** Totals for the amount eaten, using 0 for blank nutrient fields. */
    val totals: NutrientTotals
        get() = draft()?.totals() ?: NutrientTotals()

    val canSave: Boolean
        get() = name.isNotBlank() && servingsValue != null &&
            listOf(calories, carbs, sugar, sodium).all { it.isBlank() || (it.toAmount() ?: -1.0) >= 0 }

    private fun draft(): FoodEntry? {
        val s = servingsValue ?: return null
        return FoodEntry(
            id = if (isNew) 0 else entryId,
            name = name.trim(),
            timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            meal = meal,
            servingSize = servingSize.trim(),
            servings = s,
            caloriesPerServing = calories.toAmount() ?: 0.0,
            carbsPerServingG = carbs.toAmount() ?: 0.0,
            sugarPerServingG = sugar.toAmount() ?: 0.0,
            sodiumPerServingMg = sodium.toAmount() ?: 0.0,
            notes = notes.trim(),
        )
    }

    fun save(onDone: () -> Unit) {
        val entry = draft() ?: return
        if (!canSave) return
        viewModelScope.launch {
            dao.upsert(entry)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (isNew) return
        viewModelScope.launch {
            dao.byId(entryId)?.let { dao.delete(it) }
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

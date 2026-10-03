package com.kenkho.nutritracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenkho.nutritracker.data.FoodEntry
import com.kenkho.nutritracker.data.NutriDatabase
import com.kenkho.nutritracker.data.NutrientTotals
import com.kenkho.nutritracker.data.localDate
import com.kenkho.nutritracker.data.sumTotals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class DaySummary(val date: LocalDate, val entryCount: Int, val totals: NutrientTotals)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = NutriDatabase.get(app).foodEntryDao()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dayEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { date ->
            val zone = ZoneId.systemDefault()
            dao.entriesBetween(
                date.atStartOfDay(zone).toInstant().toEpochMilli(),
                date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every day that has entries, newest first, with that day's totals. */
    val history: StateFlow<List<DaySummary>> = dao.allEntries()
        .map { entries ->
            entries.groupBy { it.localDate() }
                .map { (date, list) -> DaySummary(date, list.size, list.sumTotals()) }
                .sortedByDescending { it.date }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun shiftDay(days: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(days)
    }
}

package com.kenkho.nutritracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenkho.nutritracker.data.MealType
import com.kenkho.nutritracker.data.MealWithItems
import com.kenkho.nutritracker.data.localDateTime
import com.kenkho.nutritracker.data.sumTotals
import com.kenkho.nutritracker.data.totals
import com.kenkho.nutritracker.fmt
import com.kenkho.nutritracker.longDateFormatter
import com.kenkho.nutritracker.timeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    vm: DiaryViewModel,
    onAdd: (LocalDate) -> Unit,
    onOpenMeal: (Long) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val meals by vm.dayMeals.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Diary") },
                actions = {
                    TextButton(onClick = onOpenHistory) { Text("History") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(date) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add meal") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                DateBar(
                    date = date,
                    onPrev = { vm.shiftDay(-1) },
                    onNext = { vm.shiftDay(1) },
                    onPick = { pickDate = true },
                    onToday = { vm.selectDate(LocalDate.now()) },
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Day total", style = MaterialTheme.typography.titleSmall)
                        TotalsRow(meals.sumTotals(), Modifier.padding(top = 8.dp), large = true)
                    }
                }
            }
            if (meals.isEmpty()) {
                item {
                    Text(
                        "Nothing logged for this day. Tap \"Add meal\" to log a meal or snack.",
                        Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MealType.entries.forEach { meal ->
                val mealEntries = meals.filter { it.entry.meal == meal }
                if (mealEntries.isNotEmpty()) {
                    item(key = "header-$meal") {
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TotalsRow(mealEntries.sumTotals(), Modifier.padding(vertical = 4.dp))
                        }
                    }
                    items(mealEntries, key = { it.entry.id }) { m ->
                        MealCard(m, onClick = { onOpenMeal(m.entry.id) })
                    }
                }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        // The picker reports midnight UTC of the chosen day.
                        vm.selectDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun DateBar(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    onToday: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Row(
            Modifier.weight(1f).clickable(onClick = onPick).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Pick date", Modifier.padding(end = 6.dp))
            Text(
                if (date == LocalDate.now()) "Today" else date.format(longDateFormatter),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
    if (date != LocalDate.now()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onToday) { Text("Jump to today") }
        }
    }
}

@Composable
private fun MealCard(m: MealWithItems, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.displayTitle,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    m.entry.localDateTime().toLocalTime().format(timeFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            m.sortedItems.forEach { item ->
                val serving = buildString {
                    append("• ${item.name}: ")
                    append(item.servings.fmt())
                    append(if (item.servings == 1.0) " serving" else " servings")
                    if (item.servingSize.isNotBlank()) append(" × ${item.servingSize}")
                    append(" · ${item.totals().calories.fmt()} kcal")
                }
                Text(serving, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TotalsRow(m.totals(), Modifier.padding(top = 6.dp))
        }
    }
}

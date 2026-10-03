package com.kenkho.nutritracker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kenkho.nutritracker.data.MealType
import com.kenkho.nutritracker.dateFormatter
import com.kenkho.nutritracker.fmt
import com.kenkho.nutritracker.timeFormatter
import com.kenkho.nutritracker.toAmount
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(vm: EditEntryViewModel, onClose: () -> Unit, onScan: () -> Unit) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "Add food" else "Edit food") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            }
        } else Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Nutrition Facts label")
            }
            vm.scanMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Food name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                    Text(vm.dateTime.toLocalDate().format(dateFormatter))
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                    Text(vm.dateTime.toLocalTime().format(timeFormatter))
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MealType.entries.forEach { meal ->
                    FilterChip(
                        selected = vm.meal == meal,
                        onClick = { vm.meal = meal },
                        label = { Text(meal.label) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.servingSize,
                    onValueChange = { vm.servingSize = it },
                    label = { Text("Serving size") },
                    placeholder = { Text("e.g. 2/3 cup (55g)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.6f),
                )
                OutlinedTextField(
                    value = vm.servings,
                    onValueChange = { vm.servings = it },
                    label = { Text("Servings") },
                    singleLine = true,
                    isError = vm.servingsValue == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("Per serving", Modifier.weight(1.6f), style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Total (× ${vm.servingsValue?.fmt() ?: "?"})",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                        )
                    }
                    val t = vm.totals
                    NutrientRow("Calories", "kcal", vm.calories, { vm.calories = it }, t.calories)
                    NutrientRow("Carbohydrate", "g", vm.carbs, { vm.carbs = it }, t.carbsG)
                    NutrientRow("Sugars", "g", vm.sugar, { vm.sugar = it }, t.sugarG)
                    NutrientRow("Sodium", "mg", vm.sodium, { vm.sodium = it }, t.sodiumMg)
                }
            }

            OutlinedTextField(
                value = vm.notes,
                onValueChange = { vm.notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Button(
                onClick = { vm.save(onClose) },
                enabled = vm.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save to diary") }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.dateTime.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.dateTime = day.atTime(vm.dateTime.toLocalTime())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = vm.dateTime.hour, initialMinute = vm.dateTime.minute)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.dateTime = vm.dateTime.withHour(state.hour).withMinute(state.minute)
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete entry?") },
            text = { Text("\"${vm.name}\" will be removed from your diary.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onClose)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** A per-serving input with the total for the servings eaten shown next to it. */
@Composable
private fun NutrientRow(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    total: Double,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = { Text(unit) },
            singleLine = true,
            isError = value.isNotBlank() && (value.toAmount() ?: -1.0) < 0,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1.6f),
        )
        Text(
            "${total.fmt()} $unit",
            Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

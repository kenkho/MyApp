package com.kenkho.nutritracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kenkho.nutritracker.data.NutrientTotals
import com.kenkho.nutritracker.fmt

/** Calories / Carbs / Sugar / Sodium side by side. */
@Composable
fun TotalsRow(totals: NutrientTotals, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Calories", totals.calories.fmt(), "kcal", large)
        Stat("Carbs", totals.carbsG.fmt(), "g", large)
        Stat("Sugar", totals.sugarG.fmt(), "g", large)
        Stat("Sodium", totals.sodiumMg.fmt(), "mg", large)
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, large: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value $unit",
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

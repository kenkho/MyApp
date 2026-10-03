package com.kenkho.nutritracker

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** For display: "12" or "12.5". */
fun Double.fmt(): String =
    if (this % 1.0 == 0.0) String.format(Locale.getDefault(), "%,d", toLong())
    else String.format(Locale.getDefault(), "%,.1f", this)

/** For pre-filling text fields: no grouping, always "." decimal so it parses back. */
fun Double.toInput(): String =
    if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.ROOT, "%.2f", this).trimEnd('0').trimEnd('.')

/** Accepts "1.5" or "1,5". */
fun String.toAmount(): Double? = trim().replace(',', '.').toDoubleOrNull()

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

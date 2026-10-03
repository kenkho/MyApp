package com.kenkho.nutritracker.ocr

/** Values read from a Nutrition Facts label, per serving. Null means "not found". */
data class ParsedLabel(
    val servingSize: String? = null,
    val calories: Double? = null,
    val carbsG: Double? = null,
    val sugarG: Double? = null,
    val sodiumMg: Double? = null,
) {
    val foundAnything: Boolean
        get() = listOf(servingSize, calories, carbsG, sugarG, sodiumMg).any { it != null }

    /** Fills fields missing here from [other]. */
    fun orElse(other: ParsedLabel) = ParsedLabel(
        servingSize ?: other.servingSize,
        calories ?: other.calories,
        carbsG ?: other.carbsG,
        sugarG ?: other.sugarG,
        sodiumMg ?: other.sodiumMg,
    )
}

/**
 * Extracts per-serving values from OCR text of a US-style Nutrition Facts label
 * (line names as used in 21 CFR 101.9: "Serving size", "Calories", "Sodium",
 * "Total Carbohydrate", "Total Sugars"/"Sugars").
 *
 * OCR is imperfect, so results are only a starting point; the user always reviews
 * and can edit them before saving. On dual-column labels the first value found on a
 * line (the per-serving column) is used.
 */
object NutritionLabelParser {

    private val amountWithUnit = Regex("""(\d+(?:[.,]\d+)?)\s*(mg|g)\b""", RegexOption.IGNORE_CASE)
    private val bareNumberLine = Regex("""^\s*(\d{1,4})\s*$""")

    private val servingSizeKey = Regex("""serving\s*size\s*:?\s*(.*)""", RegexOption.IGNORE_CASE)
    private val caloriesFromFat = Regex("""(?:calories\s*from\s*fat|fat\s*cal\.?)\s*\d*""", RegexOption.IGNORE_CASE)
    private val caloriesKey = Regex("""\bcalories\b""", RegexOption.IGNORE_CASE)
    private val caloriesValue = Regex("""\bcalories\b\s*:?\s*(\d{1,4})\b(?!\s*%)""", RegexOption.IGNORE_CASE)
    private val sodiumKey = Regex("""\bsodium\b""", RegexOption.IGNORE_CASE)
    private val carbKey = Regex("""\b(?:total\s*)?carb(?:ohydrates?|s|\.)?""", RegexOption.IGNORE_CASE)
    private val totalSugarKey = Regex("""\btotal\s*sugars?\b""", RegexOption.IGNORE_CASE)
    private val sugarKey = Regex("""\bsugars?\b""", RegexOption.IGNORE_CASE)

    fun parse(text: String): ParsedLabel {
        val lines = text.lines().map(::normalize).filter { it.isNotBlank() }
        return ParsedLabel(
            servingSize = findServingSize(lines),
            calories = findCalories(lines),
            carbsG = findAmount(lines, carbKey, toMg = false),
            sugarG = findSugar(lines),
            sodiumMg = findAmount(lines, sodiumKey, toMg = true),
        )
    }

    /** Cleans common OCR confusions, e.g. "Og"/"Omg" (letter O) for "0g"/"0mg". */
    internal fun normalize(line: String): String =
        line.replace(Regex("""\s+"""), " ")
            .replace(Regex("""(?<=^|[\s<])[Oo](?=\s?m?g\b)"""), "0")
            .trim()

    private fun findServingSize(lines: List<String>): String? {
        for ((i, line) in lines.withIndex()) {
            val m = servingSizeKey.find(line) ?: continue
            val rest = m.groupValues[1].trim()
            val value = rest.ifEmpty { lines.getOrNull(i + 1)?.trim().orEmpty() }
            if (value.isNotEmpty()) return value
        }
        return null
    }

    private fun findCalories(lines: List<String>): Double? {
        for ((i, raw) in lines.withIndex()) {
            // Older labels print "Calories from Fat 15" next to the calories; drop that part.
            val line = raw.replace(caloriesFromFat, "").trim()
            if (!caloriesKey.containsMatchIn(line)) continue
            // Skip the %DV footnote ("2,000 calories a day ...").
            val lower = line.lowercase()
            if ("a day" in lower || "per day" in lower) continue
            caloriesValue.find(line)?.let { return it.groupValues[1].toDouble() }
            // Large-print calorie numbers are often recognised as their own line.
            if (line.trim().endsWith("calories", ignoreCase = true)) {
                for (next in lines.drop(i + 1).take(2)) {
                    bareNumberLine.find(next)?.let { return it.groupValues[1].toDouble() }
                }
            }
        }
        return null
    }

    private fun findSugar(lines: List<String>): Double? {
        findAmount(lines, totalSugarKey, toMg = false)?.let { return it }
        // Older labels just say "Sugars"; ignore "Added Sugars" and "Sugar Alcohol".
        val plain = lines.filter {
            val l = it.lowercase()
            "added" !in l && "alcohol" !in l
        }
        return findAmount(plain, sugarKey, toMg = false)
    }

    /**
     * Finds the first line matching [key] and reads the first "<number> g|mg" after it on
     * the same line, or at the start of the next line. Converts to grams, or milligrams
     * when [toMg] is true.
     */
    private fun findAmount(lines: List<String>, key: Regex, toMg: Boolean): Double? {
        for ((i, line) in lines.withIndex()) {
            val k = key.find(line) ?: continue
            val after = line.substring(k.range.last + 1)
            val m = amountWithUnit.find(after)
                ?: lines.getOrNull(i + 1)?.let { next ->
                    amountWithUnit.find(next)?.takeIf { it.range.first <= 2 }
                }
                ?: continue
            val value = parseNumber(m.groupValues[1]) ?: continue
            val unitIsMg = m.groupValues[2].equals("mg", ignoreCase = true)
            return when {
                toMg && !unitIsMg -> value * 1000
                !toMg && unitIsMg -> value / 1000
                else -> value
            }
        }
        return null
    }

    /** "1,200" -> 1200 (thousands separator), "2,5" -> 2.5 (decimal comma), "2.5" -> 2.5. */
    internal fun parseNumber(s: String): Double? {
        val cleaned = if (Regex("""^\d{1,3},\d{3}$""").matches(s)) s.replace(",", "") else s.replace(',', '.')
        return cleaned.toDoubleOrNull()
    }
}

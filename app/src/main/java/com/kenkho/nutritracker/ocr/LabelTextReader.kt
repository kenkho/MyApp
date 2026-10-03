package com.kenkho.nutritracker.ocr

import com.google.mlkit.vision.text.Text
import kotlin.math.abs
import kotlin.math.min

/**
 * ML Kit groups text into blocks, and on a Nutrition Facts label a nutrient name and its
 * amount often land in different blocks. This rebuilds visual rows by joining recognised
 * lines whose vertical centres line up, left to right.
 */
object LabelTextReader {

    fun rows(text: Text): String {
        val lines = text.textBlocks.flatMap { it.lines }.filter { it.boundingBox != null }
            .sortedBy { it.boundingBox!!.exactCenterY() }

        val rows = mutableListOf<MutableList<Text.Line>>()
        for (line in lines) {
            val box = line.boundingBox!!
            val row = rows.lastOrNull()
            val rowBox = row?.first()?.boundingBox
            if (row != null && rowBox != null &&
                abs(box.exactCenterY() - rowBox.exactCenterY()) < min(box.height(), rowBox.height()) * 0.6f
            ) {
                row += line
            } else {
                rows += mutableListOf(line)
            }
        }
        return rows.joinToString("\n") { row ->
            row.sortedBy { it.boundingBox!!.left }.joinToString(" ") { it.text }
        }
    }

    /** Parses row-rebuilt text first, falling back to ML Kit's own reading order. */
    fun parse(text: Text): ParsedLabel =
        NutritionLabelParser.parse(rows(text)).orElse(NutritionLabelParser.parse(text.text))
}

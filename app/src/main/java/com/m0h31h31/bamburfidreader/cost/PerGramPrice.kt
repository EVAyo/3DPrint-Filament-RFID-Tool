package com.m0h31h31.bamburfidreader.cost

import java.util.Locale

/** Yuan-per-gram values stored as thousandths of a yuan per gram. */
object PerGramPrice {
    fun parse(text: String): Long? {
        val t = text.trim().removePrefix("¥").trim()
        if (t.isBlank()) return null
        val value = t.toDoubleOrNull() ?: return null
        return Math.round(value * 1000)
    }

    fun toPlain(value: Long): String =
        String.format(Locale.US, "%.3f", value / 1000.0)

    fun materialCents(weightGrams: Double, pricePerGram: Long): Double =
        weightGrams * pricePerGram / 10.0
}

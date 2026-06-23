package com.m0h31h31.bamburfidreader.cost

/** 金额(分)与显示/输入互转工具。 */
object Money {
    /** 货币符号:中文环境 ¥,其它 $。 */
    fun symbol(): String =
        if (java.util.Locale.getDefault().language.equals("zh", ignoreCase = true)) "¥" else "$"

    /** 分 → "¥12.34" / "$12.34"(按当前语言)。 */
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign${symbol()}${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** 分 → "12.34"(用于输入框)。 */
    fun toPlain(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val s = "${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
        return if (cents < 0) "-$s" else s
    }

    /** "12.34"/"12" → 分;非法返回 null。 */
    fun parse(text: String): Long? {
        val t = text.trim().removePrefix("¥").removePrefix("$").trim()
        if (t.isBlank()) return null
        val d = t.toDoubleOrNull() ?: return null
        return Math.round(d * 100)
    }
}

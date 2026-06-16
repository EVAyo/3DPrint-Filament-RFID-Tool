package com.m0h31h31.bamburfidreader.data

import com.m0h31h31.bamburfidreader.model.FilamentColorEntry
import java.util.Locale

internal fun normalizeBambuColorCode(raw: String): String {
    val cleaned = raw
        .trim()
        .trimEnd('\u0000', '\u00FF')
        .uppercase(Locale.US)
    if (cleaned.isBlank()) return ""
    val code = cleaned.substringAfterLast('-').trim()
    return stripLeadingZerosInColorCode(code)
}

// 统一颜色编码中“中间多余的 0”：字母前缀后的数字部分去掉前导 0
// （如 W01 -> W1、W02 -> W2、W001 -> W1），确保带 0 与不带 0 视为同一颜色。
// 仅处理字母前缀紧跟 0 的情况：数字中间不是 0（如 W10 保持 W10）或纯数字编码
// （如 10300）不处理；且至少保留一位数字（A0 仍为 A0）。
private val COLOR_CODE_LETTER_DIGITS = Regex("^([A-Z]+)(\\d+)$")

private fun stripLeadingZerosInColorCode(code: String): String {
    val match = COLOR_CODE_LETTER_DIGITS.find(code) ?: return code
    val prefix = match.groupValues[1]
    val digits = match.groupValues[2]
    val stripped = digits.trimStart('0').ifEmpty { "0" }
    return prefix + stripped
}

internal fun findBambuFilamentMatch(
    entries: List<FilamentColorEntry>,
    filaId: String,
    rawColorCode: String
): FilamentColorEntry? {
    val normalizedFilaId = filaId.trim().uppercase(Locale.US)
    val normalizedColorCode = normalizeBambuColorCode(rawColorCode)
    if (normalizedFilaId.isBlank() || normalizedColorCode.isBlank()) return null
    return entries.firstOrNull { entry ->
        entry.filaId.trim().uppercase(Locale.US) == normalizedFilaId &&
            normalizeBambuColorCode(entry.colorCode) == normalizedColorCode
    }
}

internal fun resolveBambuBaseFilamentType(
    specificType: String,
    typeGroups: Map<String, List<String>>
): String {
    val normalizedSpecific = normalizeFilamentTypeForMatch(specificType)
    if (normalizedSpecific.isBlank()) return ""
    return typeGroups.entries.firstOrNull { (_, specifics) ->
        specifics.any { normalizeFilamentTypeForMatch(it) == normalizedSpecific }
    }?.key.orEmpty().ifBlank { specificType.trim() }
}

private fun normalizeFilamentTypeForMatch(value: String): String {
    return value
        .trim()
        .uppercase(Locale.US)
        .filter { it.isLetterOrDigit() }
}

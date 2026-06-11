package com.m0h31h31.bamburfidreader.data

import com.m0h31h31.bamburfidreader.model.FilamentColorEntry
import java.util.Locale

internal fun normalizeBambuColorCode(raw: String): String {
    val cleaned = raw
        .trim()
        .trimEnd('\u0000', '\u00FF')
        .uppercase(Locale.US)
    if (cleaned.isBlank()) return ""
    return cleaned.substringAfterLast('-').trim()
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

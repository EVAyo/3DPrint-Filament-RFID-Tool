package com.m0h31h31.bamburfidreader.cost

import android.content.Context
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.utils.ConfigManager
import org.json.JSONObject

/**
 * Seeds material price rows from the filament catalog.
 *
 * Per-gram prices are yuan/g stored as thousandths of a yuan, so 0.067 yuan/g is
 * stored as 67.
 */
object MaterialPriceSeeder {

    private val DEFAULT_PRICE_BY_TYPE = mapOf(
        "ABS" to 67L,
        "ABS-GF" to 102L,
        "ASA" to 84L,
        "ASA-Aero" to 205L,
        "ASA-CF" to 152L,
        "PA-CF" to 425L,
        "PA6-CF" to 326L,
        "PA6-GF" to 212L,
        "PAHT-CF" to 473L,
        "PC" to 173L,
        "PC FR" to 220L,
        "PET-CF" to 127L,
        "PETG Basic" to 52L,
        "PETG HF" to 64L,
        "PETG Translucent" to 76L,
        "PETG-CF" to 127L,
        "PLA Aero" to 189L,
        "PLA Basic" to 67L,
        "PLA Dynamic" to 85L,
        "PLA Galaxy" to 110L,
        "PLA Glow" to 110L,
        "PLA Impact" to 85L,
        "PLA Lite" to 55L,
        "PLA Marble" to 110L,
        "PLA Matte" to 67L,
        "PLA Metal" to 110L,
        "PLA Silk" to 76L,
        "PLA Silk+" to 67L,
        "PLA Sparkle" to 110L,
        "PLA Tough" to 169L,
        "PLA Tough+" to 72L,
        "PLA Translucent" to 76L,
        "PLA Wood" to 93L,
        "PLA-CF" to 144L,
        "PLA Pure" to 72L,
        "PPA-CF" to 683L,
        "PPA-GF" to 850L,
        "PPS-CF" to 630L,
        "PVA" to 315L,
        "Support for ABS" to 124L,
        "Support For PA PET" to 204L,
        "Support For PLA" to 124L,
        "Support For PLA-PETG" to 172L,
        "Support G" to 85L,
        "Support W" to 85L,
        "TPU 85A" to 162L,
        "TPU 90A" to 149L,
        "TPU 95A" to 140L,
        "TPU 95A HF" to 140L,
        "TPU for AMS" to 124L
    )

    private val DEFAULT_PRICE_BY_BASE = mapOf(
        "PLA" to 67L,
        "PETG" to 52L,
        "ABS" to 67L,
        "ASA" to 84L,
        "TPU" to 140L,
        "PA" to 425L,
        "PC" to 173L,
        "PET" to 127L,
        "PPA" to 683L,
        "PPS" to 630L,
        "PVA" to 315L,
        "Support" to 124L
    )

    fun seedIfEmpty(context: Context, dao: CostDao): Int {
        if (dao.materialPriceCount() > 0L) return 0
        return seed(context, dao)
    }

    fun seed(context: Context, dao: CostDao): Int {
        val reverseTypeToBase = parseTypeMapping(context)
        val colorJson = ConfigManager.getLocalConfig(context, "filaments_color_codes.json")
            ?: return 0
        var added = 0
        try {
            val data = JSONObject(colorJson).optJSONArray("data") ?: return 0
            val seen = HashSet<String>()
            for (i in 0 until data.length()) {
                val o = data.optJSONObject(i) ?: continue
                val filaId = o.optString("fila_id").trim()
                if (filaId.isBlank() || !seen.add(filaId)) continue
                val filaType = o.optString("fila_type").trim()
                val baseType = reverseTypeToBase[filaType.lowercase()]
                    ?: filaType.substringBefore(' ').trim()
                dao.insertMaterialPriceIfAbsent(
                    MaterialPrice(
                        filaId = filaId,
                        filaType = filaType,
                        baseType = baseType,
                        pricePerGCents = defaultPricePerGram(filaType, baseType)
                    )
                )
                added++
            }
        } catch (e: Exception) {
            logDebug("MaterialPriceSeeder failed: ${e.message}")
        }
        return added
    }

    fun defaultPricePerGram(filaType: String, baseType: String): Long {
        val normalizedType = normalizeType(filaType)
        DEFAULT_PRICE_BY_TYPE.entries.firstOrNull { normalizeType(it.key) == normalizedType }?.let {
            return it.value
        }
        return DEFAULT_PRICE_BY_BASE[baseType.trim()] ?: CostConfig.DEFAULT.defaultPricePerGCents
    }

    private fun normalizeType(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()

    private fun parseTypeMapping(context: Context): Map<String, String> {
        val raw = ConfigManager.getLocalConfig(context, "filaments_type_mapping.json") ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { base ->
                    val arr = json.optJSONArray(base) ?: return@forEach
                    for (i in 0 until arr.length()) {
                        val specific = arr.optString(i).trim()
                        if (specific.isNotBlank()) put(specific.lowercase(), base)
                    }
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

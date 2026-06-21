package com.m0h31h31.bamburfidreader.cost

import android.content.Context
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.utils.ConfigManager
import org.json.JSONObject

/**
 * 从 filaments_color_codes.json(fila_id ↔ fila_type)+ filaments_type_mapping.json(基础类型分组)
 * 播种耗材价表,按**具体类型**(每个 fila_id)逐个一价。幂等:已存在的 fila_id 不覆盖。
 */
object MaterialPriceSeeder {

    /** 各基础类型的默认每克价(分),仅作为初值,用户可逐条改。 */
    private val DEFAULT_PRICE_BY_BASE = mapOf(
        "PLA" to 12L,
        "PETG" to 15L,
        "ABS" to 13L,
        "ASA" to 18L,
        "TPU" to 30L,
        "PA" to 40L,
        "PC" to 35L,
        "PET" to 30L,
        "PPA" to 60L,
        "PPS" to 80L,
        "PVA" to 50L,
        "Support" to 40L
    )

    /** 若价表为空则播种。返回新增条数。 */
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
                val price = DEFAULT_PRICE_BY_BASE[baseType] ?: CostConfig.DEFAULT.defaultPricePerGCents
                dao.insertMaterialPriceIfAbsent(
                    MaterialPrice(filaId, filaType, baseType, price)
                )
                added++
            }
        } catch (e: Exception) {
            logDebug("MaterialPriceSeeder failed: ${e.message}")
        }
        return added
    }

    /** {base: [specific,...]} → {specific(lower): base}。 */
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

package com.m0h31h31.bamburfidreader.cost

import org.json.JSONArray
import org.json.JSONObject

/** CostConfig 的 JSON 编解码,整体作为一条记录存入 cost_config(key="config")。 */
object CostConfigCodec {

    fun toJson(config: CostConfig): String {
        val powers = JSONObject()
        config.devicePowerWatts.forEach { (k, v) -> powers.put(k, v) }
        val depr = JSONObject()
        config.deviceDepreciationPerHourCents.forEach { (k, v) -> depr.put(k, v) }
        val fees = JSONArray()
        config.otherFees.forEach { fee ->
            fees.put(
                JSONObject()
                    .put("name", fee.name)
                    .put("unit", fee.unit.name)
                    .put("amount", fee.amountCents)
            )
        }
        return JSONObject()
            .put("electricityPerKwhCents", config.electricityPerKwhCents)
            .put("serviceFeeCents", config.serviceFeeCents)
            .put("baseShippingCents", config.baseShippingCents)
            .put("multicolorWasteFactor", config.multicolorWasteFactor)
            .put("multicolorSurchargeCents", config.multicolorSurchargeCents)
            .put("quoteMarkup", config.quoteMarkup)
            .put("minOrderCents", config.minOrderCents)
            .put("roundingCents", config.roundingCents)
            .put("defaultPricePerGCents", config.defaultPricePerGCents)
            .put("defaultPowerWatts", config.defaultPowerWatts)
            .put("defaultDepreciationPerHourCents", config.defaultDepreciationPerHourCents)
            .put("devicePowerWatts", powers)
            .put("deviceDepreciationPerHourCents", depr)
            .put("otherFees", fees)
            .toString()
    }

    /** 缺失字段回退到 DEFAULT,保证向后兼容。 */
    fun fromJson(raw: String?): CostConfig {
        if (raw.isNullOrBlank()) return CostConfig.DEFAULT
        return try {
            val json = JSONObject(raw)
            val d = CostConfig.DEFAULT
            CostConfig(
                electricityPerKwhCents = json.optLong("electricityPerKwhCents", d.electricityPerKwhCents),
                serviceFeeCents = json.optLong("serviceFeeCents", d.serviceFeeCents),
                baseShippingCents = json.optLong("baseShippingCents", d.baseShippingCents),
                multicolorWasteFactor = json.optDouble("multicolorWasteFactor", d.multicolorWasteFactor),
                multicolorSurchargeCents = json.optLong("multicolorSurchargeCents", d.multicolorSurchargeCents),
                quoteMarkup = json.optDouble("quoteMarkup", d.quoteMarkup),
                minOrderCents = json.optLong("minOrderCents", d.minOrderCents),
                roundingCents = json.optLong("roundingCents", d.roundingCents),
                defaultPricePerGCents = json.optLong("defaultPricePerGCents", d.defaultPricePerGCents),
                defaultPowerWatts = json.optInt("defaultPowerWatts", d.defaultPowerWatts),
                defaultDepreciationPerHourCents = json.optLong("defaultDepreciationPerHourCents", d.defaultDepreciationPerHourCents),
                devicePowerWatts = json.optJSONObject("devicePowerWatts").toIntMap().ifEmpty { d.devicePowerWatts },
                deviceDepreciationPerHourCents = json.optJSONObject("deviceDepreciationPerHourCents").toLongMap(),
                otherFees = json.optJSONArray("otherFees").toFeeList()
            )
        } catch (_: Exception) {
            CostConfig.DEFAULT
        }
    }

    private fun JSONObject?.toIntMap(): Map<String, Int> {
        if (this == null) return emptyMap()
        return buildMap {
            keys().forEach { key -> put(key, optInt(key)) }
        }
    }

    private fun JSONObject?.toLongMap(): Map<String, Long> {
        if (this == null) return emptyMap()
        return buildMap {
            keys().forEach { key -> put(key, optLong(key)) }
        }
    }

    private fun JSONArray?.toFeeList(): List<OtherFee> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                val unit = runCatching { FeeUnit.valueOf(o.optString("unit", "ORDER")) }.getOrDefault(FeeUnit.ORDER)
                add(OtherFee(o.optString("name").trim(), unit, o.optLong("amount", 0L)))
            }
        }
    }
}

package com.m0h31h31.bamburfidreader.cost

import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial

/** 附加费用的计费单位。 */
enum class FeeUnit { ORDER, PLATE, SECOND }

/** 其他附加费用项。 */
data class OtherFee(
    val name: String,
    val unit: FeeUnit,
    val amountCents: Long
)

/**
 * 基础费用配置。所有金额单位为「分」(Long)。
 * 电费/折旧按机型区分,缺失机型用 default。
 */
data class CostConfig(
    val electricityPerKwhCents: Long,
    val serviceFeeCents: Long,
    val baseShippingCents: Long,
    val multicolorWasteFactor: Double,
    val multicolorSurchargeCents: Long,
    val quoteMarkup: Double,
    val minOrderCents: Long,
    val roundingCents: Long,
    val defaultPricePerGCents: Long,
    val otherFees: List<OtherFee>,
    val devicePowerWatts: Map<String, Int>,
    val deviceDepreciationPerHourCents: Map<String, Long>,
    val defaultPowerWatts: Int,
    val defaultDepreciationPerHourCents: Long
) {
    fun powerWattsFor(model: String): Int =
        devicePowerWatts[model.trim()] ?: defaultPowerWatts

    fun depreciationPerHourCentsFor(model: String): Long =
        deviceDepreciationPerHourCents[model.trim()] ?: defaultDepreciationPerHourCents

    companion object {
        /** 内置默认值(粗略,用户可在配置弹窗修改)。 */
        val DEFAULT = CostConfig(
            electricityPerKwhCents = 100,        // ¥1.00 / kWh
            serviceFeeCents = 200,               // ¥2.00 / 单
            baseShippingCents = 0,
            multicolorWasteFactor = 0.15,        // 每多一色 +15% 材料
            multicolorSurchargeCents = 0,
            quoteMarkup = 2.0,                   // 2 倍报价
            minOrderCents = 0,
            roundingCents = 0,                   // 不取整
            defaultPricePerGCents = 12,          // ¥0.12 / g 兜底
            otherFees = emptyList(),
            devicePowerWatts = mapOf(
                "A1" to 100,
                "A1 mini" to 80,
                "P1P" to 110,
                "P1S" to 120,
                "X1" to 140,
                "X1C" to 140,
                "X1E" to 150,
                "H2D" to 160
            ),
            deviceDepreciationPerHourCents = emptyMap(),
            defaultPowerWatts = 110,
            defaultDepreciationPerHourCents = 50 // ¥0.50 / h
        )
    }
}

/** 耗材每克价(分),按 fila_id。 */
data class MaterialPrice(
    val filaId: String,
    val filaType: String,
    val baseType: String,
    val pricePerGCents: Long
)

/** 本地持久化的打印任务行。 */
data class PrintTaskRow(
    val id: Long,
    val title: String,
    val coverPath: String,
    val deviceModel: String,
    val deviceName: String,
    val weightGrams: Double,
    val costTimeSeconds: Int,
    val startTimeMillis: Long,
    val status: Int,
    val failedType: Int,
    val repetitions: Int,
    val materials: List<BambuCloudTaskMaterial>,
    val computedCostCents: Long,
    val orderId: Long?
) {
    /** 是否失败/未成功(用于统计默认排除)。 */
    val isFailed: Boolean get() = failedType != 0 || status !in SUCCESS_STATUSES

    companion object {
        /** 已知的成功状态码(保守:以观察到的 2=成功为准,可后续补充)。 */
        val SUCCESS_STATUSES = setOf(2)
    }
}

/** 订单(可由多条任务聚合)。 */
data class PrintOrder(
    val id: Long,
    val name: String,
    val actualChargeCents: Long,
    val note: String,
    val createdAt: Long
)

/** 单条任务的成本拆解(分)。 */
data class CostBreakdown(
    val materialCents: Long,
    val electricityCents: Long,
    val depreciationCents: Long,
    val multicolorCents: Long
) {
    val totalCents: Long get() = materialCents + electricityCents + depreciationCents + multicolorCents
}

/** 一笔订单的聚合视图(用于列表与统计)。 */
data class OrderView(
    val orderId: Long?,             // null = 单条任务自成一单
    val name: String,
    val tasks: List<PrintTaskRow>,
    val actualChargeCents: Long
) {
    val costCents: Long get() = tasks.sumOf { it.computedCostCents }
    val anyFailed: Boolean get() = tasks.any { it.isFailed }
    val profitCents: Long get() = actualChargeCents - costCents
    val marginPercent: Double
        get() = if (actualChargeCents > 0) profitCents.toDouble() / actualChargeCents * 100.0 else 0.0
}

/** 顶部统计。 */
data class CostStats(
    val orderCount: Int,
    val totalCostCents: Long,
    val totalRevenueCents: Long,
    val totalProfitCents: Long,
    val marginPercent: Double,
    val avgOrderValueCents: Long
)

/** 报价器输入。 */
data class QuoteInput(
    val weightGrams: Double,
    val pricePerGCents: Long,
    val estTimeSeconds: Int,
    val deviceModel: String,
    val colorCount: Int,
    val plateCount: Int,
    val nozzleMultiplier: Double = 1.0,
    val timeMultiplier: Double = 1.0
)

/** 报价拆解(分)。 */
data class QuoteBreakdown(
    val materialCents: Long,
    val electricityCents: Long,
    val depreciationCents: Long,
    val multicolorCents: Long,
    val markedUpCents: Long,
    val serviceCents: Long,
    val shippingCents: Long,
    val otherCents: Long,
    val totalCents: Long
) {
    val productionCents: Long get() = materialCents + electricityCents + depreciationCents + multicolorCents
}

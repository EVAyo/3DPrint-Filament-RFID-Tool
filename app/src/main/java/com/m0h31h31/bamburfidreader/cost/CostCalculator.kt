package com.m0h31h31.bamburfidreader.cost

import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial

/**
 * 纯计算:历史任务成本 与 新单报价。金额单位「分」(Long)。
 * 历史成本用真实 weight/costTime,**不使用喷头/时长倍率**(避免与真实时长重复计费);
 * 倍率仅在报价器 [computeQuote] 生效。
 */
object CostCalculator {

    private fun roundCents(value: Double): Long =
        if (value.isNaN() || value.isInfinite()) 0L else Math.round(value)

    /** 不同颜色数(按非空颜色去重)。 */
    fun colorCount(materials: List<BambuCloudTaskMaterial>): Int =
        materials.map { it.color.trim().uppercase() }.filter { it.isNotBlank() }.distinct().size

    /**
     * 计算单条任务成本。
     * @param priceOf 给定 filamentId 返回每克价(分);未知耗材调用方应返回兜底价。
     */
    fun computeTaskCost(
        materials: List<BambuCloudTaskMaterial>,
        fallbackWeightGrams: Double,
        costTimeSeconds: Int,
        deviceModel: String,
        repetitions: Int,
        config: CostConfig,
        priceOf: (filaId: String) -> Long
    ): CostBreakdown {
        val reps = repetitions.coerceAtLeast(1)
        val materialCents: Double = if (materials.isNotEmpty()) {
            materials.sumOf { PerGramPrice.materialCents(it.weightGrams, priceOf(it.filamentId)) }
        } else {
            PerGramPrice.materialCents(fallbackWeightGrams, config.defaultPricePerGCents)
        }
        val hours = costTimeSeconds / 3600.0
        val electricity = config.powerWattsFor(deviceModel) / 1000.0 * hours * config.electricityPerKwhCents
        val depreciation = hours * config.depreciationPerHourCentsFor(deviceModel)
        val colors = colorCount(materials)
        val multicolor = if (colors > 1) {
            materialCents * config.multicolorWasteFactor + config.multicolorSurchargeCents
        } else {
            0.0
        }
        return CostBreakdown(
            materialCents = roundCents(materialCents * reps),
            electricityCents = roundCents(electricity * reps),
            depreciationCents = roundCents(depreciation * reps),
            multicolorCents = roundCents(multicolor * reps)
        )
    }

    /** 新单报价。 */
    fun computeQuote(input: QuoteInput, config: CostConfig): QuoteBreakdown {
        val materialCents = PerGramPrice.materialCents(input.weightGrams, input.pricePerGCents)
        val hours = input.estTimeSeconds / 3600.0
        val timeFactor = input.nozzleMultiplier * input.timeMultiplier
        val electricity = config.powerWattsFor(input.deviceModel) / 1000.0 * hours * config.electricityPerKwhCents * timeFactor
        val depreciation = hours * config.depreciationPerHourCentsFor(input.deviceModel) * timeFactor
        val multicolor = if (input.colorCount > 1) {
            materialCents * config.multicolorWasteFactor + config.multicolorSurchargeCents
        } else {
            0.0
        }
        val production = materialCents + electricity + depreciation + multicolor
        val markedUp = production * config.quoteMarkup
        val plates = input.plateCount.coerceAtLeast(1)
        val other = config.otherFees.sumOf { fee ->
            when (fee.unit) {
                FeeUnit.ORDER -> fee.amountCents.toDouble()
                FeeUnit.PLATE -> fee.amountCents.toDouble() * plates
                FeeUnit.SECOND -> fee.amountCents.toDouble() * input.estTimeSeconds
            }
        }
        var total = markedUp + config.serviceFeeCents + config.baseShippingCents + other
        if (total < config.minOrderCents) total = config.minOrderCents.toDouble()
        val totalCents = applyRounding(roundCents(total), config.roundingCents)
        return QuoteBreakdown(
            materialCents = roundCents(materialCents),
            electricityCents = roundCents(electricity),
            depreciationCents = roundCents(depreciation),
            multicolorCents = roundCents(multicolor),
            markedUpCents = roundCents(markedUp),
            serviceCents = config.serviceFeeCents,
            shippingCents = config.baseShippingCents,
            otherCents = roundCents(other),
            totalCents = totalCents
        )
    }

    /** 向上取整到 step 的整数倍(step<=0 表示不取整)。 */
    fun applyRounding(cents: Long, step: Long): Long {
        if (step <= 0L) return cents
        val r = cents % step
        return if (r == 0L) cents else cents + (step - r)
    }

    /**
     * 统计:仅计入已录入实际收费(actualCharge>0)的订单 —— 未定价的打印任务不算亏损。
     * 默认排除失败单。
     */
    fun computeStats(orders: List<OrderView>, includeFailed: Boolean): CostStats {
        val visible = orders.filter {
            it.actualChargeCents > 0 && (includeFailed || !it.anyFailed)
        }
        val cost = visible.sumOf { it.costCents }
        val revenue = visible.sumOf { it.actualChargeCents }
        val profit = revenue - cost
        val margin = if (revenue > 0) profit.toDouble() / revenue * 100.0 else 0.0
        val avg = if (visible.isNotEmpty()) revenue / visible.size else 0L
        return CostStats(
            orderCount = visible.size,
            totalCostCents = cost,
            totalRevenueCents = revenue,
            totalProfitCents = profit,
            marginPercent = margin,
            avgOrderValueCents = avg
        )
    }
}

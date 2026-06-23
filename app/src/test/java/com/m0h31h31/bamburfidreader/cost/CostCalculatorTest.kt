package com.m0h31h31.bamburfidreader.cost

import com.m0h31h31.bamburfidreader.cloud.BambuCloudTaskMaterial
import org.junit.Assert.assertEquals
import org.junit.Test

class CostCalculatorTest {

    private val config = CostConfig.DEFAULT.copy(
        electricityPerKwhCents = 100,        // ¥1/kWh
        defaultPricePerGCents = 100,         // ¥0.10/g
        multicolorWasteFactor = 0.20,
        multicolorSurchargeCents = 0,
        defaultPowerWatts = 100,
        deviceDepreciationPerHourCents = mapOf("A1" to 100L), // ¥1/h
        devicePowerWatts = mapOf("A1" to 100)
    )

    private fun mat(id: String, color: String, g: Double) =
        BambuCloudTaskMaterial(id, "PLA", color, g, 0)

    @Test
    fun singleMaterialCostUsesPerGramPriceElectricityAndDepreciation() {
        // 100g @ ¥0.20/g = ¥20.00 = 2000 分
        // 1 小时(3600s),100W=0.1kW × 1h × ¥1 = ¥0.10 = 10 分
        // 折旧 1h × ¥1 = 100 分
        val cb = CostCalculator.computeTaskCost(
            materials = listOf(mat("GFA00", "FFFFFFFF", 100.0)),
            fallbackWeightGrams = 100.0,
            costTimeSeconds = 3600,
            deviceModel = "A1",
            repetitions = 1,
            config = config,
            priceOf = { 200L }
        )
        assertEquals(2000L, cb.materialCents)
        assertEquals(10L, cb.electricityCents)
        assertEquals(100L, cb.depreciationCents)
        assertEquals(0L, cb.multicolorCents)
        assertEquals(2110L, cb.totalCents)
    }

    @Test
    fun fallbackWeightUsedWhenNoMaterials() {
        val cb = CostCalculator.computeTaskCost(
            materials = emptyList(),
            fallbackWeightGrams = 50.0,
            costTimeSeconds = 0,
            deviceModel = "A1",
            repetitions = 1,
            config = config,
            priceOf = { 999L }
        )
        // 50g × 默认 10 分/g = 500
        assertEquals(500L, cb.materialCents)
    }

    @Test
    fun multicolorAddsWasteOnMaterial() {
        val cb = CostCalculator.computeTaskCost(
            materials = listOf(mat("A", "FF0000FF", 50.0), mat("B", "00FF00FF", 50.0)),
            fallbackWeightGrams = 100.0,
            costTimeSeconds = 0,
            deviceModel = "A1",
            repetitions = 1,
            config = config,
            priceOf = { 100L }
        )
        // material = 100g × 10 = 1000; 2 色 → waste 20% × 1000 = 200
        assertEquals(1000L, cb.materialCents)
        assertEquals(200L, cb.multicolorCents)
    }

    @Test
    fun repetitionsMultiplyEverything() {
        val cb = CostCalculator.computeTaskCost(
            materials = listOf(mat("A", "FFF", 10.0)),
            fallbackWeightGrams = 10.0,
            costTimeSeconds = 3600,
            deviceModel = "A1",
            repetitions = 3,
            config = config,
            priceOf = { 100L }
        )
        assertEquals(300L, cb.materialCents)   // 10×10×3
        assertEquals(30L, cb.electricityCents) // 10×3
        assertEquals(300L, cb.depreciationCents)
    }

    @Test
    fun quoteAppliesMarkupToProductionAndAddsFixedFees() {
        val cfg = config.copy(
            quoteMarkup = 2.0,
            serviceFeeCents = 200,
            baseShippingCents = 500,
            roundingCents = 0,
            minOrderCents = 0
        )
        val q = CostCalculator.computeQuote(
            QuoteInput(
                weightGrams = 100.0,
                pricePerGCents = 200L,
                estTimeSeconds = 3600,
                deviceModel = "A1",
                colorCount = 1,
                plateCount = 1
            ),
            cfg
        )
        // production = 2000(material)+10(elec)+100(deprec) = 2110
        // markedUp = 4220; + service 200 + shipping 500 = 4920
        assertEquals(2000L, q.materialCents)
        assertEquals(4220L, q.markedUpCents)
        assertEquals(4920L, q.totalCents)
    }

    @Test
    fun quoteRoundingRoundsUpToStep() {
        val cfg = config.copy(quoteMarkup = 1.0, serviceFeeCents = 0, baseShippingCents = 0, roundingCents = 100)
        val q = CostCalculator.computeQuote(
            QuoteInput(10.0, 130L, 0, "A1", 1, 1), cfg
        )
        // material = 130 → round up to 200
        assertEquals(200L, q.totalCents)
    }

    @Test
    fun minOrderEnforced() {
        val cfg = config.copy(quoteMarkup = 1.0, serviceFeeCents = 0, baseShippingCents = 0, minOrderCents = 1000, roundingCents = 0)
        val q = CostCalculator.computeQuote(QuoteInput(1.0, 100L, 0, "A1", 1, 1), cfg)
        assertEquals(1000L, q.totalCents)
    }

    @Test
    fun statsCountOnlyPricedOrdersAndExcludeFailedByDefault() {
        val ok = task(id = 1, cost = 1000, status = 2)
        val failed = task(id = 2, cost = 2000, status = 3, failedType = 1)
        val unpriced = task(id = 3, cost = 5000, status = 2)
        val orders = listOf(
            OrderView(null, "ok", listOf(ok), actualChargeCents = 3000),
            OrderView(null, "failed", listOf(failed), actualChargeCents = 500),
            OrderView(null, "unpriced", listOf(unpriced), actualChargeCents = 0)
        )
        // 默认:排除失败 + 排除未定价 → 只剩 ok
        val excl = CostCalculator.computeStats(orders, includeFailed = false)
        assertEquals(1, excl.orderCount)
        assertEquals(1000L, excl.totalCostCents)
        assertEquals(3000L, excl.totalRevenueCents)
        assertEquals(2000L, excl.totalProfitCents)

        // 含失败:ok + failed(均已定价),unpriced 仍排除
        val incl = CostCalculator.computeStats(orders, includeFailed = true)
        assertEquals(2, incl.orderCount)
        assertEquals(3000L, incl.totalCostCents)      // 1000 + 2000
        assertEquals(3500L, incl.totalRevenueCents)   // 3000 + 500
        assertEquals(500L, incl.totalProfitCents)
    }

    @Test
    fun taskStateMapsStatusCodes() {
        assertEquals(TaskState.SUCCESS, task(1, 0, status = 2).state)
        assertEquals(TaskState.PRINTING, task(2, 0, status = 4).state)
        assertEquals(TaskState.PRINTING, task(3, 0, status = 4, failedType = 1).state) // 打印中优先
        assertEquals(TaskState.FAILED, task(4, 0, status = 3).state)
        assertEquals(TaskState.FAILED, task(5, 0, status = 2, failedType = 2).state)
    }

    private fun task(id: Long, cost: Long, status: Int, failedType: Int = 0) = PrintTaskRow(
        id = id, title = "t", coverPath = "", deviceModel = "A1", deviceName = "",
        weightGrams = 0.0, costTimeSeconds = 0, startTimeMillis = 0, status = status,
        failedType = failedType, repetitions = 1, materials = emptyList(),
        computedCostCents = cost, orderId = null
    )

    @Test
    fun moneyFormatAndParseRoundTrip() {
        assertEquals("¥12.34", Money.format(1234))
        assertEquals("-¥1.00", Money.format(-100))
        assertEquals(1234L, Money.parse("12.34"))
        assertEquals(1200L, Money.parse("¥12"))
    }
}

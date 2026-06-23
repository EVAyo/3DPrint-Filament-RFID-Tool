package com.m0h31h31.bamburfidreader.cost

enum class CostConfigImpact {
    COST_AND_QUOTE,
    QUOTE_ONLY
}

data class CostConfigFieldImpact(
    val key: String,
    val impact: CostConfigImpact
)

fun costConfigFieldImpacts(): List<CostConfigFieldImpact> = listOf(
    CostConfigFieldImpact("electricity", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("default_price", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("multicolor_waste", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("multicolor_surcharge", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("default_power", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("depreciation", CostConfigImpact.COST_AND_QUOTE),
    CostConfigFieldImpact("service_fee", CostConfigImpact.QUOTE_ONLY),
    CostConfigFieldImpact("shipping", CostConfigImpact.QUOTE_ONLY),
    CostConfigFieldImpact("quote_markup", CostConfigImpact.QUOTE_ONLY),
    CostConfigFieldImpact("min_order", CostConfigImpact.QUOTE_ONLY),
    CostConfigFieldImpact("rounding", CostConfigImpact.QUOTE_ONLY),
    CostConfigFieldImpact("other_fees", CostConfigImpact.QUOTE_ONLY)
)

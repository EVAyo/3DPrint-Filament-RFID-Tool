package com.m0h31h31.bamburfidreader.cost

import org.junit.Assert.assertEquals
import org.junit.Test

class CostConfigImpactTest {
    @Test
    fun fieldsAreSeparatedByCostAndQuoteImpact() {
        val impacts = costConfigFieldImpacts().associate { it.key to it.impact }

        listOf(
            "electricity",
            "default_price",
            "multicolor_waste",
            "multicolor_surcharge",
            "default_power",
            "depreciation"
        ).forEach { key ->
            assertEquals(CostConfigImpact.COST_AND_QUOTE, impacts[key])
        }

        listOf(
            "service_fee",
            "shipping",
            "quote_markup",
            "min_order",
            "rounding",
            "other_fees"
        ).forEach { key ->
            assertEquals(CostConfigImpact.QUOTE_ONLY, impacts[key])
        }
    }
}

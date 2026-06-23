package com.m0h31h31.bamburfidreader.cost

import org.junit.Assert.assertEquals
import org.junit.Test

class PerGramPriceTest {
    @Test
    fun parsesAndFormatsThreeDecimalYuanPerGram() {
        assertEquals(67L, PerGramPrice.parse("0.067"))
        assertEquals(850L, PerGramPrice.parse("0.850"))
        assertEquals("0.067", PerGramPrice.toPlain(67L))
        assertEquals("0.850", PerGramPrice.toPlain(850L))
    }

    @Test
    fun defaultMaterialPricesUseSpecificFilamentTypes() {
        assertEquals(67L, MaterialPriceSeeder.defaultPricePerGram("PLA Basic", "PLA"))
        assertEquals(102L, MaterialPriceSeeder.defaultPricePerGram("ABS-GF", "ABS"))
        assertEquals(850L, MaterialPriceSeeder.defaultPricePerGram("PPA-GF", "PPA"))
        assertEquals(172L, MaterialPriceSeeder.defaultPricePerGram("Support For PLA-PETG", "Support"))
    }
}

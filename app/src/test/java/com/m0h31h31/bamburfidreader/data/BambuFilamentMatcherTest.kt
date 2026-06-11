package com.m0h31h31.bamburfidreader.data

import com.m0h31h31.bamburfidreader.model.FilamentColorEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BambuFilamentMatcherTest {
    @Test
    fun normalizesRfidColorCodeByDroppingA01Prefix() {
        assertEquals("W2", normalizeBambuColorCode("A01-W2"))
        assertEquals("K00", normalizeBambuColorCode(" k00\u0000\u0000 "))
    }

    @Test
    fun matchesByFilaIdAndColorCodeOnly() {
        val black = entry(
            filaId = "GFG00",
            colorCode = "K00",
            colorValues = listOf("#000000FF")
        )
        val oldColorValueOnly = entry(
            filaId = "GFG00",
            colorCode = "W00",
            colorValues = listOf("#000000FF")
        )

        assertEquals(
            black,
            findBambuFilamentMatch(
                entries = listOf(oldColorValueOnly, black),
                filaId = "GFG00",
                rawColorCode = "A01-K00"
            )
        )
    }

    @Test
    fun doesNotFallbackToColorValuesWhenColorCodeDoesNotMatch() {
        val colorValueMatchButWrongCode = entry(
            filaId = "GFG00",
            colorCode = "W00",
            colorValues = listOf("#000000FF")
        )

        assertNull(
            findBambuFilamentMatch(
                entries = listOf(colorValueMatchButWrongCode),
                filaId = "GFG00",
                rawColorCode = "A01-K00"
            )
        )
    }

    private fun entry(
        filaId: String,
        colorCode: String,
        colorValues: List<String>
    ) = FilamentColorEntry(
        filaColorCode = "legacy-$colorCode",
        colorCode = colorCode,
        filaId = filaId,
        colorType = "single",
        filaType = "PETG Basic",
        colorNameZh = "黑色",
        colorNameEn = "Black",
        colorValues = colorValues,
        colorCount = colorValues.size
    )
}

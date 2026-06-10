package com.m0h31h31.bamburfidreader.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTagBrandDetectorTest {
    @Test
    fun crealityWinsOverFfFallback() {
        val brand = FormatTagBrandDetector.choose(
            bambuAuth = false,
            snapmakerAuth = false,
            crealityAuth = true,
            ffAuth = true
        )

        assertEquals(FormatTagBrand.CREALITY, brand)
    }

    @Test
    fun snapmakerWinsOverCrealityAndFfFallback() {
        val brand = FormatTagBrandDetector.choose(
            bambuAuth = false,
            snapmakerAuth = true,
            crealityAuth = true,
            ffAuth = true
        )

        assertEquals(FormatTagBrand.SNAPMAKER, brand)
    }

    @Test
    fun unknownWhenNoKeyAuthenticates() {
        val brand = FormatTagBrandDetector.choose(
            bambuAuth = false,
            snapmakerAuth = false,
            crealityAuth = false,
            ffAuth = false
        )

        assertNull(brand)
    }
}

package com.m0h31h31.bamburfidreader.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BambuCloudRemainingSyncerTest {
    @Test
    fun calculatesRoundedRemainingPercentFromOfficialWeights() {
        assertEquals(
            76.0f,
            BambuCloudRemainingSyncer.calculateRemainingPercent(
                netWeightGrams = 760,
                totalNetWeightGrams = 1000
            )
        )
        assertEquals(
            33.3f,
            BambuCloudRemainingSyncer.calculateRemainingPercent(
                netWeightGrams = 333,
                totalNetWeightGrams = 1000
            )
        )
    }

    @Test
    fun clampsInvalidOfficialRemainingValues() {
        assertEquals(
            100f,
            BambuCloudRemainingSyncer.calculateRemainingPercent(
                netWeightGrams = 1200,
                totalNetWeightGrams = 1000
            )
        )
        assertEquals(
            0f,
            BambuCloudRemainingSyncer.calculateRemainingPercent(
                netWeightGrams = -20,
                totalNetWeightGrams = 1000
            )
        )
        assertNull(
            BambuCloudRemainingSyncer.calculateRemainingPercent(
                netWeightGrams = 100,
                totalNetWeightGrams = 0
            )
        )
    }

    @Test
    fun normalizesOfficialRfidBeforeTrayLookup() {
        assertEquals(
            "1F78AB9554E34B46BC890D60A016E9CF",
            BambuCloudRemainingSyncer.normalizeTrayUid(" 1f78ab9554e34b46bc890d60a016e9cf ")
        )
    }
}

package com.m0h31h31.bamburfidreader.cost

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CostSyncStatusTest {
    @Test
    fun blankLastSyncMeansFirstSync() {
        assertTrue(isFirstCostSync(null))
        assertTrue(isFirstCostSync(""))
        assertTrue(isFirstCostSync("   "))
    }

    @Test
    fun savedLastSyncMeansSubsequentSync() {
        assertFalse(isFirstCostSync("1718985600000"))
    }
}

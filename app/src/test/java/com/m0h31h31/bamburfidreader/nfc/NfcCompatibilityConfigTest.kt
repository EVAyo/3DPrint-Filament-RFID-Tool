package com.m0h31h31.bamburfidreader.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcCompatibilityConfigTest {
    @Test
    fun stablePresetTradesSpeedForMoreRetriesAndWriteVerification() {
        val fast = NfcCompatibilityConfig.forMode(NfcCompatibilityMode.FAST)
        val stable = NfcCompatibilityConfig.forMode(NfcCompatibilityMode.STABLE)

        assertTrue(stable.authRetryCount > fast.authRetryCount)
        assertTrue(stable.blockRetryCount > fast.blockRetryCount)
        assertTrue(stable.postKeyDerivationDelayMs > fast.postKeyDerivationDelayMs)
        assertEquals(0L, fast.readInterBlockDelayMs)
        assertEquals(0L, stable.readInterBlockDelayMs)
        assertTrue(stable.writeInterBlockDelayMs > fast.writeInterBlockDelayMs)
        assertTrue(stable.verifyEachWriteBlock)
        assertFalse(fast.verifyEachWriteBlock)
    }

    @Test
    fun balancedPresetIsDefaultAndKeepsNfcAPollingOnly() {
        val balanced = NfcCompatibilityConfig.default()

        assertEquals(NfcCompatibilityMode.BALANCED, balanced.mode)
        assertTrue(balanced.forceNfcAOnly)
        assertTrue(balanced.mifareTimeoutMs >= 900)
        assertTrue(balanced.postConnectDelayMs >= 30)
        assertTrue(balanced.postKeyDerivationDelayMs >= 80)
    }

    @Test
    fun scorePrefersSuccessfulWriteVerifiedResult() {
        val readOnly = NfcCompatibilityTestResult(
            mode = NfcCompatibilityMode.BALANCED,
            readOk = true,
            writeOk = false,
            durationMs = 600
        )
        val writeVerified = NfcCompatibilityTestResult(
            mode = NfcCompatibilityMode.STABLE,
            readOk = true,
            writeOk = true,
            durationMs = 900
        )

        assertTrue(writeVerified.score > readOnly.score)
    }
}

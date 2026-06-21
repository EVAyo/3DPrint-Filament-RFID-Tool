package com.m0h31h31.bamburfidreader.nfc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcDispatchPolicyTest {
    @Test
    fun normalReadsAreAllowedOnReaderScreenWhenGlobalListenerIsDisabled() {
        assertTrue(
            shouldHandlePassiveNfcRead(
                globalListenerEnabled = false,
                activeRoute = "reader"
            )
        )
    }

    @Test
    fun normalReadsAreIgnoredAwayFromReaderScreenWhenGlobalListenerIsDisabled() {
        assertFalse(
            shouldHandlePassiveNfcRead(
                globalListenerEnabled = false,
                activeRoute = "misc"
            )
        )
    }

    @Test
    fun normalReadsRemainGlobalWhenGlobalListenerIsEnabled() {
        assertTrue(
            shouldHandlePassiveNfcRead(
                globalListenerEnabled = true,
                activeRoute = "tag"
            )
        )
    }

    @Test
    fun blankRouteFallsBackToReaderScreenBehavior() {
        assertTrue(
            shouldHandlePassiveNfcRead(
                globalListenerEnabled = false,
                activeRoute = ""
            )
        )
    }
}

package com.m0h31h31.bamburfidreader.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class SensitiveValueMaskerTest {
    @Test
    fun masksMiddleOfLongIdentifier() {
        assertEquals("03919D****105346", SensitiveValueMasker.maskMiddle("03919D552105346"))
    }

    @Test
    fun masksShortIdentifierConservatively() {
        assertEquals("12****", SensitiveValueMasker.maskMiddle("123456"))
    }

    @Test
    fun leavesVeryShortIdentifierUntouched() {
        assertEquals("123", SensitiveValueMasker.maskMiddle("123"))
    }
}

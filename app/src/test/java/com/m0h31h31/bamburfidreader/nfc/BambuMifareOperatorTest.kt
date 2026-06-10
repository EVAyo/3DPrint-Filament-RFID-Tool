package com.m0h31h31.bamburfidreader.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BambuMifareOperatorTest {
    @Test
    fun formatTrailerStagesUseDerivedKeyBThenResetToDefaultFf() {
        val derivedA = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val derivedB = byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F)

        val stages = BambuFormatPlanner.trailerResetStages(derivedA, derivedB)

        assertArrayEquals(derivedB, stages[0].requiredKeyB)
        assertArrayEquals(
            byteArrayOf(
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                0xFF.toByte(), 0x07, 0x80.toByte(), 0x69,
                0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
            ),
            stages[0].trailer
        )
        assertArrayEquals(derivedB, stages[1].requiredKeyB)
        assertArrayEquals(BambuFormatPlanner.defaultFfTrailer(), stages[1].trailer)
    }

    @Test
    fun keyAAuthCannotResetBambuTrailer() {
        val keyAResult = MifareClassicSession.AuthResult.Success(
            keyType = MifareClassicSession.AuthKeyType.KEY_A,
            keyIndex = 0,
            attempt = 0
        )
        val keyBResult = MifareClassicSession.AuthResult.Success(
            keyType = MifareClassicSession.AuthKeyType.KEY_B,
            keyIndex = 0,
            attempt = 0
        )

        assertFalse(BambuFormatPlanner.canResetTrailer(keyAResult))
        assertTrue(BambuFormatPlanner.canResetTrailer(keyBResult))
    }

    @Test
    fun staleTagExceptionIsDetected() {
        val stale = SecurityException("Permission Denial: Tag (ID: 5A 34 EE 3B) is out of date")
        val unrelated = SecurityException("Permission Denial: missing permission")

        assertTrue(MifareClassicSession.isStaleTagException(stale))
        assertFalse(MifareClassicSession.isStaleTagException(unrelated))
    }
}

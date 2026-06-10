package com.m0h31h31.bamburfidreader.nfc

enum class NfcCompatibilityMode {
    FAST,
    BALANCED,
    STABLE
}

data class NfcCompatibilityConfig(
    val mode: NfcCompatibilityMode,
    val presenceCheckDelayMs: Int,
    val mifareTimeoutMs: Int,
    val postConnectDelayMs: Long,
    val postKeyDerivationDelayMs: Long,
    val reconnectDelayMs: Long,
    val authRetryCount: Int,
    val blockRetryCount: Int,
    val readInterBlockDelayMs: Long,
    val writeInterBlockDelayMs: Long,
    val writeVerificationDelayMs: Long,
    val forceNfcAOnly: Boolean,
    val verifyEachWriteBlock: Boolean
) {
    val interBlockDelayMs: Long
        get() = writeInterBlockDelayMs

    companion object {
        fun default(): NfcCompatibilityConfig = forMode(NfcCompatibilityMode.BALANCED)

        fun forMode(mode: NfcCompatibilityMode): NfcCompatibilityConfig {
            return when (mode) {
                NfcCompatibilityMode.FAST -> NfcCompatibilityConfig(
                    mode = mode,
                    presenceCheckDelayMs = 125,
                    mifareTimeoutMs = 600,
                    postConnectDelayMs = 10L,
                    postKeyDerivationDelayMs = 15L,
                    reconnectDelayMs = 35L,
                    authRetryCount = 1,
                    blockRetryCount = 0,
                    readInterBlockDelayMs = 0L,
                    writeInterBlockDelayMs = 0L,
                    writeVerificationDelayMs = 0L,
                    forceNfcAOnly = true,
                    verifyEachWriteBlock = false
                )

                NfcCompatibilityMode.BALANCED -> NfcCompatibilityConfig(
                    mode = mode,
                    presenceCheckDelayMs = 180,
                    mifareTimeoutMs = 1000,
                    postConnectDelayMs = 40L,
                    postKeyDerivationDelayMs = 90L,
                    reconnectDelayMs = 75L,
                    authRetryCount = 3,
                    blockRetryCount = 1,
                    readInterBlockDelayMs = 0L,
                    writeInterBlockDelayMs = 100L,
                    writeVerificationDelayMs = 20L,
                    forceNfcAOnly = true,
                    verifyEachWriteBlock = true
                )

                NfcCompatibilityMode.STABLE -> NfcCompatibilityConfig(
                    mode = mode,
                    presenceCheckDelayMs = 250,
                    mifareTimeoutMs = 1500,
                    postConnectDelayMs = 100L,
                    postKeyDerivationDelayMs = 180L,
                    reconnectDelayMs = 150L,
                    authRetryCount = 5,
                    blockRetryCount = 3,
                    readInterBlockDelayMs = 0L,
                    writeInterBlockDelayMs = 200L,
                    writeVerificationDelayMs = 50L,
                    forceNfcAOnly = true,
                    verifyEachWriteBlock = true
                )
            }
        }
    }
}

data class NfcCompatibilityTestResult(
    val mode: NfcCompatibilityMode,
    val readOk: Boolean,
    val writeOk: Boolean,
    val durationMs: Long,
    val message: String = ""
) {
    val score: Int
        get() {
            var value = 0
            if (readOk) value += 1000
            if (writeOk) value += 800
            value -= (durationMs / 25L).toInt().coerceAtMost(250)
            return value
        }
}

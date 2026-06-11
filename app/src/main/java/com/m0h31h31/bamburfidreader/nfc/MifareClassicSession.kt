package com.m0h31h31.bamburfidreader.nfc

import android.nfc.tech.MifareClassic

object MifareClassicSession {
    enum class AuthKeyType {
        KEY_A,
        KEY_B
    }

    enum class KeyOrder {
        INTERLEAVED_BY_INDEX,
        ALL_A_THEN_ALL_B
    }

    sealed class AuthResult {
        data class Success(
            val keyType: AuthKeyType,
            val keyIndex: Int,
            val attempt: Int
        ) : AuthResult()

        data class Failure(val message: String = "") : AuthResult()
        data class StaleTag(val message: String) : AuthResult()
    }

    /** NFC 失败原因细分，用于给用户更精确的提示而非笼统的“连接已失效”。 */
    enum class NfcErrorKind {
        TAG_LOST,          // 标签移出感应区 / 丢失
        TAG_OUT_OF_DATE,   // 标签句柄过期（通常是标签换了或离开过感应区）
        TRANSCEIVE_FAILED, // 通信传输失败（信号不稳定）
        IO_ERROR,          // 其它读写 IO 错误
        OTHER              // 无法归类
    }

    fun classifyError(error: Throwable): NfcErrorKind {
        if (error is android.nfc.TagLostException) return NfcErrorKind.TAG_LOST
        val message = error.message.orEmpty()
        return when {
            message.contains("out of date", ignoreCase = true) -> NfcErrorKind.TAG_OUT_OF_DATE
            message.contains("Tag was lost", ignoreCase = true) -> NfcErrorKind.TAG_LOST
            message.contains("Transceive failed", ignoreCase = true) -> NfcErrorKind.TRANSCEIVE_FAILED
            error is java.io.IOException -> NfcErrorKind.IO_ERROR
            else -> NfcErrorKind.OTHER
        }
    }

    /** Stale 结果里只保留了 message 字符串，据此判断是“移开”还是“句柄过期”。 */
    fun classifyStaleMessage(message: String): NfcErrorKind =
        if (message.contains("out of date", ignoreCase = true)) {
            NfcErrorKind.TAG_OUT_OF_DATE
        } else {
            NfcErrorKind.TAG_LOST
        }

    /** 是否属于需要“移开标签后重新贴卡”的失效类型。 */
    fun isStaleErrorKind(kind: NfcErrorKind): Boolean =
        kind == NfcErrorKind.TAG_LOST || kind == NfcErrorKind.TAG_OUT_OF_DATE

    fun isStaleTagException(error: Throwable): Boolean = isStaleErrorKind(classifyError(error))

    fun authenticateSectorWithRetry(
        mifare: MifareClassic,
        sectorIndex: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>,
        retryCount: Int,
        reconnectDelayMs: Long,
        keyOrder: KeyOrder,
        ensureConnectedBeforeAttempt: Boolean = false,
        reconnectAfterFailedAttempt: Boolean = false,
        mifareTimeoutMs: Int = 0,
        postConnectDelayMs: Long = 0L,
        appendLog: (String, String) -> Unit = { _, _ -> }
    ): Boolean {
        return authenticateSectorWithRetryDetailed(
            mifare = mifare,
            sectorIndex = sectorIndex,
            keysA = keysA,
            keysB = keysB,
            retryCount = retryCount,
            reconnectDelayMs = reconnectDelayMs,
            keyOrder = keyOrder,
            ensureConnectedBeforeAttempt = ensureConnectedBeforeAttempt,
            reconnectAfterFailedAttempt = reconnectAfterFailedAttempt,
            mifareTimeoutMs = mifareTimeoutMs,
            postConnectDelayMs = postConnectDelayMs,
            appendLog = appendLog
        ) is AuthResult.Success
    }

    fun authenticateSectorWithRetryDetailed(
        mifare: MifareClassic,
        sectorIndex: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>,
        retryCount: Int,
        reconnectDelayMs: Long,
        keyOrder: KeyOrder,
        ensureConnectedBeforeAttempt: Boolean = false,
        reconnectAfterFailedAttempt: Boolean = false,
        mifareTimeoutMs: Int = 0,
        postConnectDelayMs: Long = 0L,
        appendLog: (String, String) -> Unit = { _, _ -> }
    ): AuthResult {
        var lastFailure = ""
        for (attempt in 0..retryCount) {
            if (
                ensureConnectedBeforeAttempt &&
                !ensureConnected(mifare, reconnectDelayMs, mifareTimeoutMs, postConnectDelayMs, appendLog)
            ) {
                lastFailure = "sector $sectorIndex connect failed"
                continue
            }

            val result = when (keyOrder) {
                KeyOrder.INTERLEAVED_BY_INDEX -> {
                    authenticateInterleavedByIndex(mifare, sectorIndex, keysA, keysB, attempt, appendLog)
                }

                KeyOrder.ALL_A_THEN_ALL_B -> {
                    authenticateAllAThenAllB(mifare, sectorIndex, keysA, keysB, attempt, appendLog)
                }
            }
            when (result) {
                is AuthResult.Success -> return result
                is AuthResult.StaleTag -> return result
                is AuthResult.Failure -> lastFailure = result.message
            }

            when (keyOrder) {
                KeyOrder.INTERLEAVED_BY_INDEX -> {
                    if (attempt < retryCount && reconnectAfterFailedAttempt) {
                        appendLog("D", "sector $sectorIndex auth failed, reconnect retry ${attempt + 1}")
                        if (!reconnect(mifare, reconnectDelayMs, mifareTimeoutMs, postConnectDelayMs, appendLog)) {
                            lastFailure = "sector $sectorIndex reconnect failed"
                        }
                    }
                }

                KeyOrder.ALL_A_THEN_ALL_B -> {
                    if (attempt < retryCount) {
                        if (!reconnect(mifare, reconnectDelayMs, mifareTimeoutMs, postConnectDelayMs, appendLog)) {
                            lastFailure = "sector $sectorIndex reconnect failed"
                        }
                    }
                }
            }
        }
        return AuthResult.Failure(lastFailure)
    }

    fun ensureConnected(
        mifare: MifareClassic,
        reconnectDelayMs: Long,
        mifareTimeoutMs: Int = 0,
        postConnectDelayMs: Long = 0L,
        appendLog: (String, String) -> Unit = { _, _ -> }
    ): Boolean {
        return if (mifare.isConnected) {
            applyTimeout(mifare, mifareTimeoutMs, appendLog)
            true
        } else {
            reconnect(mifare, reconnectDelayMs, mifareTimeoutMs, postConnectDelayMs, appendLog)
        }
    }

    fun reconnect(
        mifare: MifareClassic,
        reconnectDelayMs: Long,
        mifareTimeoutMs: Int = 0,
        postConnectDelayMs: Long = 0L,
        appendLog: (String, String) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            try {
                mifare.close()
            } catch (_: Exception) {
            }
            Thread.sleep(reconnectDelayMs)
            mifare.connect()
            applyTimeout(mifare, mifareTimeoutMs, appendLog)
            Thread.sleep(if (postConnectDelayMs > 0) postConnectDelayMs else reconnectDelayMs)
            true
        } catch (e: Exception) {
            appendLog("W", "reconnect failed: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    fun applyTimeout(
        mifare: MifareClassic,
        mifareTimeoutMs: Int,
        appendLog: (String, String) -> Unit = { _, _ -> }
    ) {
        if (mifareTimeoutMs <= 0) return
        try {
            mifare.timeout = mifareTimeoutMs
        } catch (e: Exception) {
            appendLog("D", "set MIFARE timeout failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun authenticateInterleavedByIndex(
        mifare: MifareClassic,
        sectorIndex: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>,
        attempt: Int,
        appendLog: (String, String) -> Unit
    ): AuthResult {
        var lastFailure = ""
        for (keyIdx in 0 until maxOf(keysA.size, keysB.size)) {
            val keyA = keysA.getOrNull(keyIdx)
            val keyB = keysB.getOrNull(keyIdx)
            val label = if (keyIdx == 0) "primary" else "backup"

            try {
                val okA = keyA != null && mifare.authenticateSectorWithKeyA(sectorIndex, keyA)
                if (okA) {
                    if (attempt > 0 || keyIdx > 0) {
                        appendLog("I", "sector $sectorIndex auth success attempt=$attempt keySet=$label KeyA")
                    }
                    return AuthResult.Success(AuthKeyType.KEY_A, keyIdx, attempt)
                }

                val okB = keyB != null && mifare.authenticateSectorWithKeyB(sectorIndex, keyB)
                if (okB) {
                    if (attempt > 0 || keyIdx > 0) {
                        appendLog("I", "sector $sectorIndex auth success attempt=$attempt keySet=$label KeyB")
                    }
                    return AuthResult.Success(AuthKeyType.KEY_B, keyIdx, attempt)
                }

                lastFailure = "sector $sectorIndex auth false attempt=$attempt keySet=$label"
                appendLog(
                    "D",
                    "sector $sectorIndex auth false attempt=$attempt keySet=$label (KeyA=${keyA != null} KeyB=${keyB != null})"
                )
            } catch (e: Exception) {
                if (isStaleTagException(e)) {
                    return AuthResult.StaleTag(e.message.orEmpty())
                }
                lastFailure = "sector $sectorIndex auth exception attempt=$attempt keySet=$label: ${e.message.orEmpty()}"
                appendLog(
                    "D",
                    "sector $sectorIndex auth exception attempt=$attempt keySet=$label: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
        return AuthResult.Failure(lastFailure)
    }

    private fun authenticateAllAThenAllB(
        mifare: MifareClassic,
        sectorIndex: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>,
        attempt: Int,
        appendLog: (String, String) -> Unit
    ): AuthResult {
        var lastFailure = ""
        keysA.forEachIndexed { index, key ->
            if (key != null) {
                try {
                    if (mifare.authenticateSectorWithKeyA(sectorIndex, key)) {
                        return AuthResult.Success(AuthKeyType.KEY_A, index, attempt)
                    }
                    lastFailure = "sector $sectorIndex KeyA auth false index=$index"
                } catch (e: Exception) {
                    if (isStaleTagException(e)) return AuthResult.StaleTag(e.message.orEmpty())
                    lastFailure = "sector $sectorIndex KeyA auth exception index=$index: ${e.message.orEmpty()}"
                    appendLog("D", "$lastFailure")
                }
            }
        }
        keysB.forEachIndexed { index, key ->
            if (key != null) {
                try {
                    if (mifare.authenticateSectorWithKeyB(sectorIndex, key)) {
                        return AuthResult.Success(AuthKeyType.KEY_B, index, attempt)
                    }
                    lastFailure = "sector $sectorIndex KeyB auth false index=$index"
                } catch (e: Exception) {
                    if (isStaleTagException(e)) return AuthResult.StaleTag(e.message.orEmpty())
                    lastFailure = "sector $sectorIndex KeyB auth exception index=$index: ${e.message.orEmpty()}"
                    appendLog("D", "$lastFailure")
                }
            }
        }
        return AuthResult.Failure(lastFailure)
    }
}

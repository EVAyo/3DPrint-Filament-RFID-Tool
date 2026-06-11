package com.m0h31h31.bamburfidreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MiscStatusToneTest {
    @Test
    fun nfcCompatibilityWriteSuccessUsesSuccessTone() {
        val message = "NFC 兼容性测试成功，推荐并已应用：稳定。快速:读OK/写OK 120ms"

        assertEquals(StatusTone.SUCCESS, resolveStatusTone(message))
    }

    @Test
    fun nfcCompatibilityWriteFailureDoesNotUseSuccessTone() {
        val message = "NFC 写入兼容性测试未找到可靠写入模式；只读可用。快速:读OK/写失败 120ms"

        assertEquals(StatusTone.ERROR, resolveStatusTone(message))
    }
}

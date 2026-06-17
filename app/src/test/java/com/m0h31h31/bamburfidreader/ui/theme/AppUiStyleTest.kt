package com.m0h31h31.bamburfidreader.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUiStyleTest {
    @Test
    fun defaultStyleUsesModernWorkbench() {
        assertEquals(AppUiStyle.MODERN_WORKBENCH, DEFAULT_APP_UI_STYLE)
    }

    @Test
    fun styleOptionsKeepExistingStylesAndAddModernWorkbench() {
        val styles = AppUiStyle.entries.toSet()

        assertTrue(styles.contains(AppUiStyle.NEUMORPHIC))
        assertTrue(styles.contains(AppUiStyle.MIUIX))
        assertTrue(styles.contains(AppUiStyle.MODERN_WORKBENCH))
    }
}

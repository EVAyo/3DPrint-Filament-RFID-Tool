package com.m0h31h31.bamburfidreader.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BambuPrinterImageMatcherTest {
    private val assets = listOf(
        "Bambu Lab A1 mini_cover.png",
        "Bambu Lab P1S_cover.png",
        "Bambu Lab X1 Carbon_cover.png"
    )

    @Test
    fun matchesProductNameToCoverAsset() {
        val printer = BambuCloudPrinter(
            deviceId = "printer-1",
            deviceName = "Studio",
            modelName = "BL-P001",
            productName = "X1 Carbon",
            online = true,
            taskId = "",
            taskName = "",
            taskStatus = "",
            progress = null,
            thumbnailUrl = ""
        )

        val match = BambuPrinterImageMatcher.matchAssetName(printer, assets)

        assertEquals("Bambu Lab X1 Carbon_cover.png", match)
    }

    @Test
    fun fallsBackToDeviceNameAliasWhenProductNameIsMissing() {
        val printer = BambuCloudPrinter(
            deviceId = "printer-2",
            deviceName = "Desk A1 mini",
            modelName = "",
            productName = "",
            online = true,
            taskId = "",
            taskName = "",
            taskStatus = "",
            progress = null,
            thumbnailUrl = ""
        )

        val match = BambuPrinterImageMatcher.matchAssetName(printer, assets)

        assertEquals("Bambu Lab A1 mini_cover.png", match)
    }

    @Test
    fun returnsNullWhenNoAssetMatches() {
        val printer = BambuCloudPrinter(
            deviceId = "printer-3",
            deviceName = "Unknown",
            modelName = "",
            productName = "",
            online = false,
            taskId = "",
            taskName = "",
            taskStatus = "",
            progress = null,
            thumbnailUrl = ""
        )

        val match = BambuPrinterImageMatcher.matchAssetName(printer, assets)

        assertNull(match)
    }
}

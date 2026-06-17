package com.m0h31h31.bamburfidreader.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class BambuCloudFilamentCatalogTest {
    @Test
    fun normalizesTrayIdNameColorCodeForLocalCatalogLookup() {
        assertEquals("D1", BambuCloudFilamentCatalog.normalizeTrayColorCode("A00-D01"))
        assertEquals("C0", BambuCloudFilamentCatalog.normalizeTrayColorCode("G01-C0"))
        assertEquals("W2", BambuCloudFilamentCatalog.normalizeTrayColorCode("A01-W02"))
    }
}

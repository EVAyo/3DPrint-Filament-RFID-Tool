package com.m0h31h31.bamburfidreader.ui.screens

import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudFilamentDetailTest {
    @Test
    fun limitCloudFilamentNoteKeepsNotesAtOrBelowLimit() {
        assertEquals("short note", limitCloudFilamentNote("short note"))
        assertEquals(150, limitCloudFilamentNote("a".repeat(150)).length)
    }

    @Test
    fun limitCloudFilamentNoteTruncatesNotesAboveLimit() {
        val result = limitCloudFilamentNote("b".repeat(151))

        assertEquals(150, result.length)
        assertEquals("b".repeat(150), result)
    }

    @Test
    fun buildCloudFilamentUpdateReturnsNoChangeWhenEditableValuesAreUnchanged() {
        val result = buildCloudFilamentUpdate(
            filament = filament(),
            displayName = "Shelf A",
            netWeightText = "120",
            note = "ready"
        )

        assertTrue(result is CloudFilamentUpdateBuildResult.NoChanges)
    }

    @Test
    fun buildCloudFilamentUpdateIncludesOnlyChangedEditableValues() {
        val result = buildCloudFilamentUpdate(
            filament = filament(),
            displayName = "Shelf B",
            netWeightText = "120",
            note = "ready"
        )

        assertTrue(result is CloudFilamentUpdateBuildResult.Update)
        val update = (result as CloudFilamentUpdateBuildResult.Update).update
        assertEquals(10L, update.id)
        assertEquals("Shelf B", update.displayName)
        assertEquals(null, update.netWeightGrams)
        assertEquals(null, update.note)
    }

    @Test
    fun buildCloudFilamentUpdateRejectsNetWeightOutsideValidRange() {
        val tooHeavy = buildCloudFilamentUpdate(
            filament = filament(),
            displayName = "Shelf A",
            netWeightText = "201",
            note = "ready"
        )
        val negative = buildCloudFilamentUpdate(
            filament = filament(),
            displayName = "Shelf A",
            netWeightText = "-1",
            note = "ready"
        )

        assertTrue(tooHeavy is CloudFilamentUpdateBuildResult.InvalidNetWeight)
        assertTrue(negative is CloudFilamentUpdateBuildResult.InvalidNetWeight)
    }

    private fun filament(): BambuCloudFilament {
        return BambuCloudFilament(
            id = 10L,
            createType = "ams",
            vendor = "Bambu Lab",
            type = "PLA",
            name = "PLA Basic",
            filamentId = "GFA00",
            rfid = "rfid",
            color = "#000000FF",
            colors = listOf("#000000FF"),
            netWeightGrams = 120,
            totalNetWeightGrams = 200,
            note = "ready",
            trayIdName = "A00-K0",
            displayName = "Shelf A",
            inPrinter = false,
            deviceId = "",
            amsSerial = "",
            slotId = "",
            amsId = null
        )
    }
}

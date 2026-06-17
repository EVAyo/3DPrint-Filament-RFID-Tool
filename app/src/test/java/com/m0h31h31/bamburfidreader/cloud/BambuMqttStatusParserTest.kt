package com.m0h31h31.bamburfidreader.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BambuMqttStatusParserTest {
    @Test
    fun parsesPrintReportIntoRealtimeStatus() {
        val payload = """
            {
              "print": {
                "command": "push_status",
                "gcode_state": "RUNNING",
                "subtask_name": "Dragon.3mf",
                "task_id": "task-1",
                "mc_percent": 43,
                "mc_remaining_time": 87,
                "nozzle_temper": 220.5,
                "nozzle_target_temper": 230,
                "bed_temper": 60,
                "bed_target_temper": 65,
                "chamber_temper": 31.2,
                "wifi_signal": "-47dBm",
                "ams": {
                  "ams": [
                    {
                      "humidity": "4",
                      "id": "0",
                      "temp": "22.7",
                      "tray": [
                        { "id": "0" },
                        {
                          "id": "1",
                          "cols": [ "DFE2E3FF" ],
                          "tray_color": "DFE2E3FF",
                          "tray_type": "PLA",
                          "tray_info_idx": "GFA05",
                          "tray_id_name": "Basic PLA",
                          "remain": 88,
                          "nozzle_temp_min": "190",
                          "nozzle_temp_max": "240",
                          "tag_uid": "01020304"
                        }
                      ]
                    }
                  ],
                  "tray_now": "1",
                  "tray_exist_bits": "2"
                }
              }
            }
        """.trimIndent()

        val status = BambuMqttStatusParser.parseReport("printer-1", payload)

        requireNotNull(status)
        assertEquals("printer-1", status.deviceId)
        assertEquals("RUNNING", status.gcodeState)
        assertEquals("Dragon.3mf", status.taskName)
        assertEquals("task-1", status.taskId)
        assertEquals(43, status.progress)
        assertEquals(87, status.remainingMinutes)
        assertEquals(220.5, status.nozzleTemperature!!, 0.01)
        assertEquals(230.0, status.nozzleTargetTemperature!!, 0.01)
        assertEquals(60.0, status.bedTemperature!!, 0.01)
        assertEquals(65.0, status.bedTargetTemperature!!, 0.01)
        assertEquals(31.2, status.chamberTemperature!!, 0.01)
        assertEquals("-47dBm", status.wifiSignal)
        assertEquals("1", status.currentTray)
        assertEquals(1, status.amsUnits.size)
        assertEquals("0", status.amsUnits[0].id)
        assertEquals("4", status.amsUnits[0].humidity)
        assertEquals("22.7", status.amsUnits[0].temperature)
        assertEquals(2, status.amsUnits[0].trays.size)
        assertEquals("1", status.amsUnits[0].trays[1].id)
        assertEquals("DFE2E3FF", status.amsUnits[0].trays[1].color)
        assertEquals("PLA", status.amsUnits[0].trays[1].filamentType)
        assertEquals("Basic PLA", status.amsUnits[0].trays[1].name)
        assertEquals(88, status.amsUnits[0].trays[1].remain)
        assertEquals("190", status.amsUnits[0].trays[1].nozzleTempMin)
        assertEquals("240", status.amsUnits[0].trays[1].nozzleTempMax)
        assertEquals("01020304", status.amsUnits[0].trays[1].tagUid)
    }

    @Test
    fun returnsNullWhenPayloadHasNoPrintObject() {
        val status = BambuMqttStatusParser.parseReport("printer-1", """{"system":{}}""")

        assertNull(status)
    }
}

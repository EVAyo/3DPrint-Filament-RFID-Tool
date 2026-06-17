package com.m0h31h31.bamburfidreader.cloud

import org.json.JSONObject

object BambuMqttStatusParser {
    fun parseReport(
        deviceId: String,
        payload: String,
        nowMillis: Long = System.currentTimeMillis()
    ): BambuPrinterRealtimeStatus? {
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        val print = root.optJSONObject("print") ?: return null
        return BambuPrinterRealtimeStatus(
            deviceId = deviceId,
            gcodeState = print.optCleanString("gcode_state"),
            taskName = print.optCleanString("subtask_name")
                .ifBlank { print.optCleanString("task_name") },
            taskId = print.optCleanString("task_id"),
            progress = print.optNullableInt("mc_percent")?.coerceIn(0, 100),
            remainingMinutes = print.optNullableInt("mc_remaining_time"),
            nozzleTemperature = print.optNullableDouble("nozzle_temper"),
            nozzleTargetTemperature = print.optNullableDouble("nozzle_target_temper"),
            bedTemperature = print.optNullableDouble("bed_temper"),
            bedTargetTemperature = print.optNullableDouble("bed_target_temper"),
            chamberTemperature = print.optNullableDouble("chamber_temper"),
            wifiSignal = print.optCleanString("wifi_signal"),
            currentTray = print.optJSONObject("ams")?.optCleanString("tray_now").orEmpty(),
            amsUnits = parseAmsUnits(print.optJSONObject("ams")),
            updatedAtMillis = nowMillis
        )
    }

    private fun parseAmsUnits(amsRoot: JSONObject?): List<BambuAmsUnit> {
        val amsArray = amsRoot?.optJSONArray("ams") ?: return emptyList()
        return buildList {
            for (amsIndex in 0 until amsArray.length()) {
                val ams = amsArray.optJSONObject(amsIndex) ?: continue
                val trays = ams.optJSONArray("tray")
                add(
                    BambuAmsUnit(
                        id = ams.optCleanString("id").ifBlank { amsIndex.toString() },
                        humidity = ams.optCleanString("humidity"),
                        temperature = ams.optCleanString("temp"),
                        trays = buildList {
                            if (trays == null) return@buildList
                            for (trayIndex in 0 until trays.length()) {
                                val tray = trays.optJSONObject(trayIndex) ?: continue
                                add(
                                    BambuAmsTray(
                                        id = tray.optCleanString("id").ifBlank { trayIndex.toString() },
                                        name = tray.optCleanString("tray_id_name"),
                                        filamentType = tray.optCleanString("tray_type"),
                                        color = tray.optCleanString("tray_color")
                                            .ifBlank { tray.optFirstColor() },
                                        remain = tray.optNullableInt("remain"),
                                        trayInfoIndex = tray.optCleanString("tray_info_idx"),
                                        nozzleTempMin = tray.optCleanString("nozzle_temp_min"),
                                        nozzleTempMax = tray.optCleanString("nozzle_temp_max"),
                                        tagUid = tray.optCleanString("tag_uid")
                                    )
                                )
                            }
                        }
                    )
                )
            }
        }
    }

    private fun JSONObject.optCleanString(key: String): String {
        if (!has(key) || isNull(key)) return ""
        return optString(key).trim()
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return runCatching { optInt(key) }.getOrNull()
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return runCatching { optDouble(key) }.getOrNull()
    }

    private fun JSONObject.optFirstColor(): String {
        val colors = optJSONArray("cols") ?: return ""
        return colors.optString(0).trim()
    }
}

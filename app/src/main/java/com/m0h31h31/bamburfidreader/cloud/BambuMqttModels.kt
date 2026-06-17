package com.m0h31h31.bamburfidreader.cloud

data class BambuPrinterRealtimeStatus(
    val deviceId: String,
    val gcodeState: String,
    val taskName: String,
    val taskId: String,
    val progress: Int?,
    val remainingMinutes: Int?,
    val nozzleTemperature: Double?,
    val nozzleTargetTemperature: Double?,
    val bedTemperature: Double?,
    val bedTargetTemperature: Double?,
    val chamberTemperature: Double?,
    val wifiSignal: String,
    val currentTray: String,
    val amsUnits: List<BambuAmsUnit>,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class BambuAmsUnit(
    val id: String,
    val humidity: String,
    val temperature: String,
    val trays: List<BambuAmsTray>
)

data class BambuAmsTray(
    val id: String,
    val name: String,
    val filamentType: String,
    val color: String,
    val remain: Int?,
    val trayInfoIndex: String,
    val nozzleTempMin: String,
    val nozzleTempMax: String,
    val tagUid: String
) {
    val hasFilament: Boolean
        get() = filamentType.isNotBlank() ||
            name.isNotBlank() ||
            color.isNotBlank() ||
            tagUid.isNotBlank()
}

enum class BambuMqttConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

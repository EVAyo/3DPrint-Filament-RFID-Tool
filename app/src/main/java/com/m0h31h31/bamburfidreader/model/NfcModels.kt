package com.m0h31h31.bamburfidreader.model

const val DEFAULT_REMAINING_PERCENT = 100

data class NfcUiState(
    val status: String,
    val uidHex: String = "",
    val keyA0Hex: String = "",
    val keyB0Hex: String = "",
    val keyA1Hex: String = "",
    val keyB1Hex: String = "",
    val blockHexes: List<String> = List(8) { "" },
    val parsedFields: List<ParsedField> = emptyList(),
    val displayType: String = "",
    val displayColorName: String = "",
    val displayColorCode: String = "",
    val displayFilaColorCode: String = "",
    val displayColorType: String = "",
    val displayColors: List<String> = emptyList(),
    val secondaryFields: List<ParsedField> = emptyList(),
    val trayUidHex: String = "",
    val remainingPercent: Float = DEFAULT_REMAINING_PERCENT.toFloat(),
    val remainingGrams: Int = 0,
    val totalWeightGrams: Int = 0,
    val originalMaterial: String = "",
    val notes: String = "",
    val error: String = ""
)

data class ParsedField(
    val label: String,
    val value: String
)

data class DisplayData(
    val type: String,
    val detailedType: String = "",
    val colorName: String,
    val colorNameEn: String = "",
    val filaColorCode: String = "",
    val colorCode: String,
    val colorType: String,
    val colorValues: List<String>,
    val secondaryFields: List<ParsedField>
)

data class ParsedBlockData(
    val fields: List<ParsedField>,
    val materialId: String,
    val colorCode: String = "",
    val filamentType: String = "",
    val detailedFilamentType: String = "",
    val colorValues: List<String>
)

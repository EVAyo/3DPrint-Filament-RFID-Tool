package com.m0h31h31.bamburfidreader.model

import java.util.Locale

data class FilamentColorEntry(
    val colorCode: String,
    val filaId: String,
    val colorType: String,
    val filaType: String,
    val filaDetailedType: String = "",
    val colorNameZh: String,
    val colorNameEn: String = "",
    val colorValues: List<String>,
    val colorCount: Int
) {
    fun resolvedColorName(): String {
        val lang = Locale.getDefault().language.lowercase(Locale.US)
        return if (lang == "zh") colorNameZh else colorNameEn.ifBlank { colorNameZh }
    }
}

data class InventoryItem(
    val trayUid: String,
    val materialType: String,
    val materialDetailedType: String = "",
    val colorName: String,
    val colorNameEn: String = "",
    val colorCode: String,
    val colorType: String,
    val colorValues: List<String>,
    val remainingPercent: Float,
    val remainingGrams: Int? = null,
    val originalMaterial: String = "",
    val notes: String = ""
) {
    fun resolvedColorName(): String {
        val lang = Locale.getDefault().language.lowercase(Locale.US)
        return if (lang == "zh") colorName else colorNameEn.ifBlank { colorName }
    }
}

data class ShareTagDbMeta(
    val id: Long = -1L,
    val copyCount: Int = 0,
    val verified: Boolean = false
)

data class ShareTagDbRow(
    val id: Long,
    val fileUid: String,
    val trayUid: String?,
    val materialType: String?,
    val colorUid: String?,
    val colorName: String?,
    val colorNameEn: String?,
    val colorType: String?,
    val colorValues: String?,
    val rawData: String?,
    val copyCount: Int,
    val verified: Boolean,
    val productionDate: String?
)

data class ShareTagItem(
    val relativePath: String,
    val fileName: String,
    val sourceUid: String,
    val trayUid: String,
    val materialType: String,
    val colorUid: String,
    val colorName: String,
    val colorNameEn: String = "",
    val colorType: String,
    val colorValues: List<String>,
    val rawBlocks: List<ByteArray?>,
    val dbId: Long = -1L,
    val copyCount: Int = 0,
    val verified: Boolean = false,
    val productionDate: String = ""
) {
    fun resolvedColorName(): String {
        val lang = Locale.getDefault().language.lowercase(Locale.US)
        return if (lang == "zh") colorName else colorNameEn.ifBlank { colorName }
    }
}

data class CModifyRecoveryInfo(
    val originalUid: String,
    val targetUid: String,
    val originalKeysA: List<String>,
    val originalKeysB: List<String>,
    val targetKeysA: List<String>,
    val targetKeysB: List<String>
)

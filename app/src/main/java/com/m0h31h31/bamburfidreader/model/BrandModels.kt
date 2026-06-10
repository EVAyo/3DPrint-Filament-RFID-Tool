package com.m0h31h31.bamburfidreader.model

data class CrealityMaterial(
    val materialId: String,
    val brand: String,
    val materialType: String,
    val name: String,
    val minTemp: Int,
    val maxTemp: Int,
    val diameter: String
)

data class CrealityTagData(
    val materialId: String,
    val colorHex: String,
    val weight: String,
    val serial: String,
    val vendorId: String,
    val batch: String,
    val lengthCode: String,
    val rawPlaintext: String,
    val uidHex: String = "",
    val mfDate: String = ""
)

data class SnapmakerTagData(
    val vendor: String,
    val manufacturer: String,
    val mainType: String,
    val subType: String,
    val colorCount: Int,
    val rgb1: Int,
    val rgb2: Int,
    val rgb3: Int,
    val rgb4: Int,
    val rgb5: Int,
    val diameter: Int,   // unit: 0.01 mm, e.g. 175 = 1.75 mm
    val weight: Int,     // grams
    val dryingTemp: Int,
    val dryingTime: Int, // hours
    val hotendMaxTemp: Int,
    val hotendMinTemp: Int,
    val bedTemp: Int,
    val mfDate: String,
    val isOfficial: Boolean,
    val uidHex: String,
    val rsaKeyVersion: Int
)

enum class ReaderBrand { BAMBU, CREALITY, SNAPMAKER }

data class SnapmakerShareTagItem(
    val uid: String,
    val vendor: String,
    val manufacturer: String,
    val mainType: Int,
    val subType: Int = 0,
    val diameter: Int,   // unit: 0.01 mm
    val weight: Int,     // grams
    val rgb1: Int,
    val mfDate: String,
    val rawBlocks: List<ByteArray?>,
    val dbId: Long = -1L,
    val copyCount: Int = 0
)

data class CrealityWritePending(
    val materialId: String,
    val colorHex: String,
    val weight: String
)

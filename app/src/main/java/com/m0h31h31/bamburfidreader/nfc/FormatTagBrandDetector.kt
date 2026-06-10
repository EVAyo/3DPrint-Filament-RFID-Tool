package com.m0h31h31.bamburfidreader.nfc

enum class FormatTagBrand {
    BAMBU,
    SNAPMAKER,
    CREALITY,
    DEFAULT_FF
}

object FormatTagBrandDetector {
    fun choose(
        bambuAuth: Boolean,
        snapmakerAuth: Boolean,
        crealityAuth: Boolean,
        ffAuth: Boolean
    ): FormatTagBrand? {
        return when {
            bambuAuth -> FormatTagBrand.BAMBU
            snapmakerAuth -> FormatTagBrand.SNAPMAKER
            crealityAuth -> FormatTagBrand.CREALITY
            ffAuth -> FormatTagBrand.DEFAULT_FF
            else -> null
        }
    }
}

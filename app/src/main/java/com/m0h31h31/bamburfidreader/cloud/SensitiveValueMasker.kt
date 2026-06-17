package com.m0h31h31.bamburfidreader.cloud

object SensitiveValueMasker {
    fun maskMiddle(value: String): String {
        val clean = value.trim()
        if (clean.length <= 4) return clean
        if (clean.length <= 8) {
            return clean.take(2) + "****"
        }
        return clean.take(6) + "****" + clean.takeLast(6)
    }
}

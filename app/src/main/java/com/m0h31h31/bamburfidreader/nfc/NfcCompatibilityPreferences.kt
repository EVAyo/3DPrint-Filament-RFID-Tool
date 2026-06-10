package com.m0h31h31.bamburfidreader.nfc

import android.content.Context

object NfcCompatibilityPreferences {
    private const val PREFS_NAME = "nfc_compatibility_prefs"
    private const val KEY_MODE = "mode"

    fun load(context: Context): NfcCompatibilityConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = runCatching {
            NfcCompatibilityMode.valueOf(
                prefs.getString(KEY_MODE, NfcCompatibilityMode.BALANCED.name).orEmpty()
            )
        }.getOrDefault(NfcCompatibilityMode.BALANCED)
        return NfcCompatibilityConfig.forMode(mode)
    }

    fun saveMode(context: Context, mode: NfcCompatibilityMode): NfcCompatibilityConfig {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
        return NfcCompatibilityConfig.forMode(mode)
    }
}

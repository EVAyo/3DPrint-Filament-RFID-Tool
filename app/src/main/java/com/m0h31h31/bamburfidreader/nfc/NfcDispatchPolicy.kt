package com.m0h31h31.bamburfidreader.nfc

fun shouldHandlePassiveNfcRead(
    globalListenerEnabled: Boolean,
    activeRoute: String
): Boolean {
    return globalListenerEnabled || activeRoute.isBlank() || activeRoute == "reader"
}
